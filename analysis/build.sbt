import sbt._
import Keys._

name := "lift"

//Exercise
lazy val analysisExercise = project.in(file("analysis-exercise"))

//The main aggregate
lazy val root = (project in file(".")).aggregate(analysisExercise)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
