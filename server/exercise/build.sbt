import Dependencies._

Build.Settings.project

name := "exercise"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  akka.streams.core,
  // Breeze
  scalanlp.breeze,
  scalanlp.natives, // FIXME: for our needs, which is more efficient: jBLAS; native packaged BLAS; or commercial BLAS?
  scalanlp.nak,
  // For REST API
  spray.httpx,
  spray.can,
  spray.routing,
  // Json
  json4s.native,
  // Codec
  scodec_bits,
  scalaz.core,
  // Parsing
  parboiled,
  // Apple push notifications
  apns,
  slf4j_simple,
  // For improving future based chaining
  async,
  // Testing
  scalatest % "test",
  scalacheck % "test",
  akka.testkit % "test",
  spray.testkit % "test",
  akka.streams.testkit % "test"
)
