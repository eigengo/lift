import Dependencies._

Build.Settings.project

name := "exercise-dl"

libraryDependencies ++= Seq(
  // Spark
  spark.core,
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
