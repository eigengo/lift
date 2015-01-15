import Dependencies._

Build.Settings.project

name := "kafka"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  // Json
  json4s.native,
  // Codec
  slf4j_simple,
  //Kafka dependencies
  kafka.kafka,
  // Testing
  scalatest % "test",
  scalacheck % "test",
  akka.testkit % "test"
)