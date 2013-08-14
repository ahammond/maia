import sbt._
import sbt.Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object MaiaBuild extends Build {

  lazy val maia = Project(
    id = "maia",
    base = file("."),
    settings = assemblySettings ++ Project.defaultSettings ++ Seq(
      name := "Maia",
      organization := "com.mindflakes.maia.hipchat",
      version := "1.0.2",
      scalaVersion := "2.10.0",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
        libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.1.2",
        "com.typesafe" % "config" % "1.0.0",
        "org.igniterealtime.smack" % "smackx" % "3.2.1",
        "org.igniterealtime.smack" % "smack" % "3.2.1",
        "com.ning" % "async-http-client" % "1.7.19",
        "ch.qos.logback" % "logback-classic" % "1.0.3"
      )
    )
  )
}
