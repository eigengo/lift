import Dependencies._

Build.Settings.project

name := "lift-common"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  spray.routing,
  json4s.native,
  scodec_bits,
  spray.can,
  spray.routing
)
