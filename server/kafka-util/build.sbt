import Dependencies._

Build.Settings.project

name := "kafkaUtil"

libraryDependencies ++= Seq(
  typesafeConfig,
  kafka.kafka,
  slf4j_simple,
  scalaz.core
)