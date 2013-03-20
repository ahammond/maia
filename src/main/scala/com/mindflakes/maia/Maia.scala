package com.mindflakes.maia

import akka.actor._
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events._
import com.typesafe.config.ConfigFactory

case class PlayPause()
case class NowPlaying()
case class Tired()
case class Hate()
case class Love()
case class Respond(message: String)

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[MaiaIRCActor],"irc")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()
}

class MaiaIRCActor extends Actor with ActorLogging {
  val cfg = ConfigFactory.load()
  val irc_bot = new PircBotX
  irc_bot.setName(cfg.getString("maia.nick"))
  irc_bot.setAutoNickChange(true)
  irc_bot.connect(cfg.getString("maia.host"))
  irc_bot.joinChannel(cfg.getString("maia.channel"))
  irc_bot.setAutoReconnect(true)
  irc_bot.getListenerManager.addListener(new LogAdapter)
  val logger = context.actorOf(Props[MaiaIRCLogger],"logger")
  val hermes = context.actorOf(Props[MaiaHermes],"hermes")
  val trigger = context.actorOf(Props(new MaiaTriggerActor(cfg.getString("maia.trigger"))),"trigger")


  override def postStop() {
    irc_bot.setAutoReconnect(false)
    irc_bot.shutdown()
  }

  def receive = {
    case Respond(msg) => {
      irc_bot.sendMessage(cfg.getString("maia.channel"), msg)
    }
    case _ => {}
  }

  class LogAdapter extends ListenerAdapter {
    override def onMessage(event: MessageEvent[Nothing]) {
      context.system.eventStream.publish(event)
    }
  }

}

class MaiaIRCLogger extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])
  def receive = {
    case MessageEvent(chan,user,msg) => {
      log.info(s"|$chan| <$user> $msg")
    }
    case _ => {}
  }
}

class MaiaTriggerActor(trigger: String) extends Actor with ActorLogging {
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
            case "love" => {
              context.actorFor(hermes) ! Love
            }
            case "np" => {
              context.actorFor(hermes) ! NowPlaying
            }
            case "help" => {
              context.actorFor("/user/irc") ! Respond("For help, please see the README.md @ https://github.com/crazysim/maia .")
            }
            case _ => {
              context.actorFor("/user/irc") ! Respond("Unknown Command")
            }
          }
        }
      }
    }
    case _ => {}
  }
}

class MaiaHermes extends Actor with ActorLogging with ActorAppleScript {
  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])

  def hermes(command: String): String = {
    log.info("Command: " + command)
    val res = ascript("tell application \"Hermes\" to " + command)
    res
  }

  def title = hermes("get title of current song")
  def artist = hermes("get artist of current song")
  def album = hermes("get album of current song")
  def titleURL = hermes("get titleURL of current song")
  def playbackState = hermes("get playback state")
  def stationName = hermes("get name of current station")

  import org.pircbotx.Colors._
  def np = s"$BOLD•Title: $NORMAL $title $BOLD •Artist: $NORMAL $artist $BOLD •Album: $NORMAL $album $BOLD •Station: $NORMAL $stationName"

  def respond(msg: String) {
    context.actorFor("/user/irc") ! Respond(msg)
  }

  def receive = {
    case PlayPause => {
      hermes("playpause")
      respond(s"Hermes is now $playbackState.")
    }
    case Tired => {
      respond(s"$title by $artist banned for a month.")
      hermes("tired of song")
    }
    case Hate => {
      respond(s"$title by $artist banned and sound-alikes discouraged on $stationName.")
      hermes("thumbs down")

    }
    case Love => {
      respond(s"$title by $artist loved and sound-alikes encouraged om $stationName.")
      hermes("thumbs up")

    }
    case NowPlaying => {
      respond("Now Playing: " + np)
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