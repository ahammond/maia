package com.mindflakes.maia

import akka.actor._
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events._
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext

case class PlayPause()
case class NowPlaying()
case class Tired()
case class Hate()
case class Skip()
case class Love()
case class Respond(message: String)

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
  val irc_bot = new PircBotX
  irc_bot.setVerbose(true)
  irc_bot.setName(cfg.getString("maia.nick"))
  irc_bot.setAutoNickChange(true)
  irc_bot.setAutoReconnect(true)
  irc_bot.setAutoReconnectChannels(true)

  irc_bot.getListenerManager.addListener(new LogAdapter)

  irc_bot.connect(cfg.getString("maia.host"))
  if (cfg.hasPath("maia.auth")) {
    irc_bot.identify(cfg.getString("maia.auth"))
  }

  //  Workaround for late identify response.
  val s = self
  context.system.scheduler.scheduleOnce(10.seconds) {
    s ! JoinChannel(cfg.getString("maia.channel"))
  }

  val logger = context.actorOf(Props[IRCLogger],"logger")
  val hermes = context.actorOf(Props[Hermes],"hermes")
  val trigger = context.actorOf(Props(new TriggerHandler(cfg.getString("maia.trigger"))),"trigger")

  class LogAdapter extends ListenerAdapter {
    override def onMessage(event: MessageEvent[Nothing]) {
      context.system.eventStream.publish(event)
    }
  }

  override def postStop() {
    irc_bot.shutdown(true)
  }

  def receive = {
    case Respond(msg) => {
      irc_bot.sendMessage(cfg.getString("maia.channel"), msg)
    }
    case JoinChannel(msg) => {
      log.info(s"Joining $msg")
      irc_bot.joinChannel(msg)
    }
  }
}

class IRCLogger extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])
  def receive = {
    case MessageEvent(chan,user,msg) => {
      log.info(s"|$chan| <$user> $msg")
    }
    case _ => {}
  }
}

class TriggerHandler(trigger: String) extends Actor with ActorLogging {
  val hermes = "/user/irc/hermes"

  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])

  def receive = {
    case MessageEvent(_,_,message) => {
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
  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])

  def hermes(command: String): String = {
    log.info("Command: " + command)
    val res = ascript("tell application \"Hermes\" to " + command)
    res
  }

  import org.pircbotx.Colors._

  def title = BOLD + hermes("get title of current song") + NORMAL
  def artist = BOLD + hermes("get artist of current song") + NORMAL
  def album = BOLD + hermes("get album of current song") + NORMAL
  def titleURL = hermes("get titleURL of current song")
  def playbackState = hermes("get playback state")
  def stationName = BOLD + hermes("get name of current station") + NORMAL

  def np = s"$title by $artist from $album on $stationName "

  def respond(msg: String) {
    context.actorFor("/user/irc") ! Respond(msg)
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
    case _ => {}
  }
}

object MessageEvent {
  def unapply(m: MessageEvent[_]): Option[(String, String, String)] = {
    Some(m.getChannel.getName, m.getUser.getNick, m.getMessage)
  }
}

trait ActorAppleScript {
  def ascript(script: String): String = {
    val runtime = Runtime.getRuntime
    val args = Array("osascript", "-e", script)
    val result = runtime.exec(args)
    scala.io.Source.fromInputStream(result.getInputStream).getLines().mkString("")
  }
}
