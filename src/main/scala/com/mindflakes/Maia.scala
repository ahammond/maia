package com.mindflakes

import akka.actor._
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

case class Message()

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[MaiaIRCActor],"irc")
  val logger = system.actorOf(Props[MaiaIRCLogger],"logger")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()
}

class MaiaIRCActor extends Actor with ActorLogging {
  val irc_bot = new PircBotX
  irc_bot.setName("MaiaIRCActor")
  irc_bot.connect("irc.rizon.net")
  irc_bot.joinChannel("#gardening")
  irc_bot.sendMessage("#gardening", "Itzza me, an actor")
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
  context.system.eventStream.subscribe(self, classOf[MessageEvent[PircBotX]])
  def receive = {
    case e: MessageEvent[PircBotX] => {
      log.info(e.getMessage)
    }
    case _ => {}
  }
}
