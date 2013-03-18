package com.mindflakes

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import org.jibble.pircbot.PircBot

case class Start()


class MaiaIRCActor extends PircBot with Actor with ActorLogging {
  setName("MaiaIRCActor")
  def receive = {
    case Start => {
      log.info("Starting")
    }
  }
}


object Maia extends App {
  val system = ActorSystem("Maia")
  val irc_bot = system.actorOf(Props[MaiaIRCActor], "IRCBot")

  irc_bot ! Start
  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()

}
