package com.mindflakes.maia

import akka.actor._
import com.typesafe.config.ConfigFactory
import scala.concurrent.{Future, ExecutionContext}
import org.jivesoftware.smack.{PacketListener, ConnectionConfiguration, XMPPConnection}
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smack.packet.Packet
import scala.io.Source
import com.ning.http.client.{RequestBuilder, AsyncHttpClient}
import scala.util.parsing.json.JSON

case class PlayPause()
case class NowPlaying()
case class Tired()
case class Hate()
case class Skip()
case class Love()

case class StationSearch(query: String)

case class StationSelect(query: String)

case class Respond(message: String)

case class RoomMessage(str: String)

case class Station(name: String, id: String)

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[IRCBot],"irc")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()
}

object IRCBot {
  case class JoinChannel(channel: String)
}

class IRCBot extends Actor with ActorLogging {
  import IRCBot._
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val cfg = ConfigFactory.load()

  val bot = self

  val logger = context.actorOf(Props[IRCLogger],"logger")
  val hermes = context.actorOf(Props[Hermes],"hermes")
  val trigger = context.actorOf(Props(new TriggerHandler(cfg.getString("maia.trigger"))),"trigger")

  val roomId = cfg.getString("maia.room")

  val config = new ConnectionConfiguration("chat.hipchat.com", 5222)
  val connection = new XMPPConnection(config)
  connection.connect()
  connection.login(cfg.getString("maia.user"), cfg.getString("maia.password"), "bot")
  val chat = new MultiUserChat(connection, cfg.getString("maia.room"))
  chat.join(cfg.getString("maia.realname"))

  chat.addMessageListener(new PacketListener {
    def processPacket(packet: Packet) {
      import org.jivesoftware.smack.packet.Message
      packet match {
        case m: Message => {
          context.system.eventStream.publish(RoomMessage(m.getBody))
        }
        case _ => {

        }
      }
    }
  })
  val client = new AsyncHttpClient()

  def receive = {
    case Respond(msg) => {
      val builder = new RequestBuilder("POST")
      val request = builder.setUrl("http://api.hipchat.com/v1/rooms/message")
        .addParameter("auth_token", cfg.getString("maia.key"))
        .addParameter("room_id", cfg.getString("maia.roomId"))
        .addParameter("from", cfg.getString("maia.from"))
        .addParameter("message", msg)
        .build()
      client.executeRequest(request)
    }
    case JoinChannel(msg) => {

    }
  }
}

class IRCLogger extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[RoomMessage])
  def receive = {
    case RoomMessage(msg) => {
      log.info(s"$msg")
    }
    case _ => {}
  }
}

class TriggerHandler(trigger: String) extends Actor with ActorLogging {
  val hermes = "/user/irc/hermes"

  context.system.eventStream.subscribe(self, classOf[RoomMessage])

  def receive = {
    case RoomMessage(message) => {
      message.take(trigger.length) match {
        case `trigger` => {
          message.drop(trigger.length) match {
            case "playpause" | "pauseplay" | "pp" => {
              context.actorFor(hermes) ! PlayPause
            }
            case "tired" => {
              context.actorFor(hermes) ! Tired
            }
            case "hate" => {
              context.actorFor(hermes) ! Hate
            }
            case "love" | "like" => {
              context.actorFor(hermes) ! Love
            }
            case "np" => {
              context.actorFor(hermes) ! NowPlaying
            }
            case "skip" => {
              context.actorFor(hermes) ! Skip
            }
            case "help" => {
              context.actorFor("/user/irc") ! Respond("For help, please see the README.md @ https://github.com/crazysim/maia .")
            }
            case s if s.startsWith("search ") => {
              val query = message.drop(trigger.length + "search ".length)
              context.actorFor(hermes) ! StationSearch(query)
            }
            case _ => {
              context.actorFor("/user/irc") ! Respond("Unknown Command")
            }
          }
        }
        case _ => {}
      }
    }
    case _ => {}
  }
}

class Hermes extends Actor with ActorLogging with ActorAppleScript {

  def hermes(command: String): String = {
    log.info("Command: " + command)
    val res = ascript("tell application \"Hermes\" to " + command)
    xml.Utility.escape(res)
  }

  def hermesStrict(command: String): String = {
    log.info("Command: " + command)
    val res = ascript_strict("tell application \"Hermes\" to " + command)
    xml.Utility.escape(res)
  }

  def bold(s: String) = {
    "<b>" + s + "</b>"
  }

  def title = bold(hermes("get title of current song"))
  def artist = hermes("get artist of current song")
  def album = hermes("get album of current song")
  def titleURL = hermes("get titleURL of current song")
  def artURL = hermes("get art of current song")
  def playbackState = bold(hermes("get playback state"))
  def stationName = bold(hermes("get name of current station"))

  def help = "!!like, !!skip, or !!hate"

  def np = s"<table>" +
    s"<tr>" +
    s"<td><img src=$artURL height='96px'><td>" +
    s"<td>Now $playbackState on $stationName:<br/><br/>$title <br/>by <i>$artist</i> <br/>from <i>$album</i>" +
    s"<tr>" +
    s"</table>"

  def respond(msg: String) {
    context.actorFor("/user/irc") ! Respond(msg)
  }

  def search(msg: String): Option[List[Station]] = {

    // Hacky JSON conversion
    val station_names_as = hermesStrict("name of stations")
    val station_ids_as = hermesStrict("stationId of stations")
    val station_names_json = "[" + station_names_as.drop(1).dropRight(1) + "]"
    val station_ids_json = "[" + station_ids_as.drop(1).dropRight(1) + "]"

    val station_names = JSON.parseFull(station_names_json)
    val station_ids = JSON.parseFull(station_ids_json)

    (station_names, station_ids) match {
      case (a: Some[Any], b: Some[Any]) => {
        val names = a.get.asInstanceOf[List[String]]
        val ids = b.get.asInstanceOf[List[String]]
        val stations = (names, ids).zipped.map( (name, id) =>
          Station(name ,id)
        )
        Some(stations)
      }
      case _ => {
        None
      }
    }
  }

  def receive = {
    case PlayPause => {
      hermes("playpause")
      respond(s"Hermes is now $playbackState.")
    }
    case Tired => {
      respond(s"$title by $artist banned for a month on $stationName.")
      hermes("tired of song")
    }
    case Hate => {
      respond(s"$title by $artist banned and sound-alikes discouraged on $stationName.")
      hermes("thumbs down")
    }
    case Love => {
      respond(s"$title by $artist loved and sound-alikes encouraged on $stationName.")
      hermes("thumbs up")
    }
    case Skip => {
      respond(s"$title by $artist skipped")
      hermes("next song")
    }
    case NowPlaying => {
      respond(np)
    }
    case StationSearch(query) => {
      val result = search(query)
      respond(result.toString)
    }
    case StationSelect(query) => {

    }
    case _ => {}
  }
}

trait ActorAppleScript {
  def ascript(script: String): String = {
    val runtime = Runtime.getRuntime
    val args = Array("osascript", "-e", script)
    val result = runtime.exec(args)
    scala.io.Source.fromInputStream(result.getInputStream).getLines().mkString("")
  }

  def ascript_strict(script: String): String = {
    val runtime = Runtime.getRuntime
    val args = Array("osascript", "-s s", "-e", script)
    val result = runtime.exec(args)
    scala.io.Source.fromInputStream(result.getInputStream).getLines().mkString("")
  }
}
