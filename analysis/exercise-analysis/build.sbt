import Dependencies._

Build.Settings.project

name := "lift-analysis-exercise"

libraryDependencies ++= Seq(
  // Spark
  spark.core,
  spark.streaming,
  spark.streaming_kafka,
  // Kafka
  kafka.kafka,
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
