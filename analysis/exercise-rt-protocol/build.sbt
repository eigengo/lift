import Dependencies._

Build.Settings.project

name := "exercise-rt-protocol"

libraryDependencies ++= Seq(
  protostuff.core,
  protostuff.runtime,
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
