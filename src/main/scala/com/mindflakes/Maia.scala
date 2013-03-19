package com.mindflakes

import akka.actor._
import org.pircbotx.PircBotX

object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = new MaiaIRCBot(system)

  println("Press 'Return' key to exit.")
  readLine()
  irc_bot.shutdown()
  system.shutdown()
}

class MaiaIRCBot(system: ActorSystem) extends PircBotX {
  setName("MaiaIRCBot")
  connect("irc.rizon.net")
  joinChannel("#gardening")
  log("Joined")
}
