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
      organization := "com.mindflakes.maia",
      version := "0.5-SNAPSHOT",
      scalaVersion := "2.10.0",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.1.2",
        "org.pircbotx" % "pircbotx" % "1.9",
        "com.typesafe" % "config" % "1.0.0"
      )
    )
  )
}
