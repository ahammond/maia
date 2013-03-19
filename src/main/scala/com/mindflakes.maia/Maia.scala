package com.mindflakes.maia

import akka.actor._
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import java.io.{BufferedReader, InputStreamReader, PrintStream}

case class Message()
case class PlayPause()
case class NowPlaying()

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[MaiaIRCActor],"irc")
  val logger = system.actorOf(Props[MaiaIRCLogger],"logger")
  val hermes = system.actorOf(Props[MaiaHermes],"hermes")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()
}

class MaiaIRCActor extends Actor with ActorLogging {
  val irc_bot = new PircBotX
  irc_bot.setName("MaiaIRCActor")
  irc_bot.connect("irc.rizon.net")
  irc_bot.joinChannel("#gardening")
  irc_bot.getListenerManager.addListener(new LogAdapter)

  override def postStop() {
    irc_bot.shutdown()
  }

  class LogAdapter extends ListenerAdapter {
    override def onMessage(event: MessageEvent[Nothing]) {
      context.system.eventStream.publish(event)
    }
  }

  def receive = {
    case _ => {}
  }
}

class MaiaIRCLogger extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])
  def receive = {
    case e: MessageEvent[_] => {
      val chan_name = e.getChannel.getName
      val msg = e.getMessage
      log.info(s"$chan_name $msg")
    }
    case _ => {}
  }
}

class MaiaHermes extends Actor with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[MessageEvent[_]])

  def ascript(script: String): String = {
    val runtime = Runtime.getRuntime
    val args = Array("osascript", "-e", script)
    val result = runtime.exec(args)
    scala.io.Source.fromInputStream(result.getInputStream).getLines().mkString("")
  }

  def hermes(command: String): String = {
    ascript("tell application \"Hermes\" to " + command)
  }

  def receive = {
    case m: MessageEvent[_] if m.getMessage.equals("playpause") => {
      log.info("playpause message recieved")
      self ! PlayPause
    }
    case PlayPause => {
      hermes("playpause")
      log.info("NP: " + hermes("get {title, artist, album} of current song"))
    }
    case NowPlaying => {
      sender ! ""
    }
    case _ => {}
  }

}
