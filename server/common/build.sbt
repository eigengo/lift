import Dependencies._

Build.Settings.project

name := "common"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  scalaz.core,
  cassandra_driver,
  // Spray + Json
  spray.routing,
  spray.can,
  // Scodec
  scodec_bits,
  json4s.native
)
