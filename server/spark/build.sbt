import Dependencies._

Build.Settings.project

name := "spark"

version := "1.0"

libraryDependencies ++= Seq(
  spark.core,
  spark.mllib,
  spark.streaming,
  spark.streamingKafka
)