package com.mindflakes

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout

case object Tick
case object Get

class Counter extends Actor with ActorLogging {
  var count = 0

  def receive = {
    case Tick => {
      log.info("tick")
      count += 1
    }
    case Get  => sender ! count
  }
}

object Maia extends App {
  val system = ActorSystem("Maia")
  implicit val ec = system.dispatcher

  val counter = system.actorOf(Props[Counter], "counter")

  counter ! Tick
  counter ! Tick
  counter ! Tick

  implicit val timeout = Timeout(5.seconds)

  (counter ? Get) onSuccess {
    case count => println("Count is " + count)
  }

  system.shutdown()
}
