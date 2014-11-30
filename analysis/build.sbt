import sbt._
import Keys._

name := "lift"

//Common code, but not protocols
lazy val common = project.in(file("common"))

//Exercise
lazy val exercise = project.in(file("exercise-analysis")).dependsOn(common)

//The main aggregate
lazy val root = (project in file(".")).aggregate(exercise, common)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
