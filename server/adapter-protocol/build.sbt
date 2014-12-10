import Dependencies._

Build.Settings.project

name := "lift-adapter"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  // For REST API
  spray.httpx,
  spray.can,
  spray.routing,
  // Testing
)
