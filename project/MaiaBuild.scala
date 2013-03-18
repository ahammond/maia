import sbt._
import sbt.Keys._

object MaiaBuild extends Build {

  lazy val maia = Project(
    id = "maia",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Maia",
      organization := "com.mindflakes",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.1.2",
        "pircbot" % "pircbot" % "1.5.0",
        "com.typesafe" % "config" % "1.0.0"
      )
    )
  )
}
