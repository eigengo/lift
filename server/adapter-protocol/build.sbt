import Dependencies._

Build.Settings.project

name := "lift-adapter-protocol"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib
)
