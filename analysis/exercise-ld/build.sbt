import Dependencies._

Build.Settings.project

name := "exercise-ld"

libraryDependencies ++= Seq(
  // Spark
  spark.streaming_kafka,
  // Kafka
  kafka.kafka,
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
