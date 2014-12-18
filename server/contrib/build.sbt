import Dependencies._

Build.Settings.project

name := "contrib"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  // For REST API
  spray.httpx,
  spray.can,
  spray.routing,
  spray.client,
  json4s.native,
  // Testing
  scalatest % "test",
  scalacheck % "test",
  akka.testkit % "test",
  spray.testkit % "test"
)
