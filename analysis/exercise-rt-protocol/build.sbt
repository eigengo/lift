import Dependencies._

Build.Settings.project

name := "exercise-rt-protocol"

crossScalaVersions := Seq("2.10.4", "2.11.4")

libraryDependencies ++= Seq(
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
