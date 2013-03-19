package com.mindflakes.maia

import akka.actor._
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent
import java.io.{BufferedReader, InputStreamReader, PrintStream}

case class Message()
case class PlayPause()
case class NowPlaying()
case class Tired()
case class Hate()
case class Respond(message: String)

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[MaiaIRCActor],"irc")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()
}

class MaiaIRCActor extends Actor with ActorLogging {
  val irc_bot = new PircBotX
  irc_bot.setName("MaiaIRCActor")
  irc_bot.connect("irc.rizon.net")
  irc_bot.joinChannel("#gardening")
  irc_bot.setAutoReconnect(true)
  irc_bot.setAutoNickChange(true)
  irc_bot.getListenerManager.addListener(new LogAdapter)
  val logger = context.actorOf(Props[MaiaIRCLogger],"logger")
  val hermes = context.actorOf(Props[MaiaHermes],"hermes")

  override def postStop() {
    irc_bot.shutdown()
  }

  def receive = {
    case Respond(msg) => {
      irc_bot.sendMessage("#gardening", msg)
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
    case m: MessageEvent[_] if m.getMessage.equals("!!!playpause") => {
      log.info("playpause message recieved")
      self ! PlayPause
    }
    case m: MessageEvent[_] if m.getMessage.equals("!!!tired") => {
      log.info("tired recieved")
      self ! Tired
    }
    case m: MessageEvent[_] if m.getMessage.equals("!!!hate") => {
      log.info("hate recieved")
      self ! Hate
    }
    case m: MessageEvent[_] if m.getMessage.equals("!!!np") => {
      log.info("hate recieved")
      self ! NowPlaying
    }
    case PlayPause => {
      hermes("playpause")
      log.info("NP: " + hermes("get {title, artist, album} of current song"))
    }
    case Tired => {
      hermes("tired of song")
      log.info("NP: " + hermes("get {title, artist, album} of current song"))
    }
    case Hate => {
      hermes("thumbs down")
      log.info("NP: " + hermes("get {title, artist, album} of current song"))
    }
    case NowPlaying => {
      context.actorFor(self.path.parent) ! Respond(hermes("get {title, artist, album} of current song"))
    }
    case _ => {}
  }

}
