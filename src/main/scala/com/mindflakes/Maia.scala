package com.mindflakes

import akka.actor._
import org.pircbotx.PircBotX

case class Message()

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[MaiaIRCActor],"irc")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()
}

class MaiaIRCActor extends Actor {
  val irc_bot = new PircBotX
  irc_bot.setName("MaiaIRCActor")
  irc_bot.connect("irc.rizon.net")
  irc_bot.joinChannel("#gardening")
  irc_bot.sendMessage("#gardening", "Itzza me, an actor")

  override def postStop() {
    irc_bot.shutdown()
  }

  def receive = {
    case _ => {}
  }
}
