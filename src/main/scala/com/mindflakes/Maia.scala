package com.mindflakes

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

case class Start()

class MaiaIRCActor extends Actor with ActorLogging {
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

}
