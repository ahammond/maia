package com.mindflakes

import akka.actor._
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import org.jibble.pircbot.PircBot






object Maia extends App {
  val system = ActorSystem("Maia")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()

}
