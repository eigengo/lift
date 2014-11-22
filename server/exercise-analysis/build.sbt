import Dependencies._

Build.Settings.project

name := "lift-exercise-analysis"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  // Spark
  spark.core,
  spark.streaming,
  // Testing
  scalatest % "test",
  scalacheck % "test",
  akka.testkit % "test",
  spray.testkit % "test"
)
