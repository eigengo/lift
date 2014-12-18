import Dependencies._

Build.Settings.project

name := "exercise"

libraryDependencies ++= Seq(
  // Codec
  scodec_bits,
  scalaz.core,
  // Testing
  scalatest % "test",
  scalacheck % "test"
)
