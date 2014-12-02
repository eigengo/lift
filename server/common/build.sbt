import Dependencies._

Build.Settings.project

name := "lift-common"

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
  json4s.native,
  // Scodec
  scodec_bits,
  // Etcd client
  etcd_client
)
