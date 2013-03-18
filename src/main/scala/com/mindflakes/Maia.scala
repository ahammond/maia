package com.mindflakes

import akka.actor._

object Maia extends App {
  val system = ActorSystem("Maia")

  println("Press 'Return' key to exit.")
  readLine()
  system.shutdown()

}
