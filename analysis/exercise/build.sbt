import Dependencies._

Build.Settings.project

name := "exercise"

libraryDependencies ++= Seq(
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
