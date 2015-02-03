import Dependencies._

Build.Settings.project

name := "kafkaUtil"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  typesafeConfig,
  kafka.kafka,
  slf4j_simple,
  scalaz.core
)