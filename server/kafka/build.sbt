import Dependencies._

Build.Settings.project

name := "kafka"

libraryDependencies ++= Seq(
  akka.actor,
  kafka.kafka,
  slf4j_simple,
  scalaz.core
)