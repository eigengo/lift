import Dependencies._

Build.Settings.project

name := "kafka"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  slf4j_simple,
  kafka.kafka
)