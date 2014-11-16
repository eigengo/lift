import sbt._
import Keys._

name := "lift"

lazy val build = Project("build", file("build"))

lazy val common = Project("common", file("common")).dependsOn(build)

lazy val exercise = Project("exercise", file("exercise")).dependsOn(build, notification, common)

lazy val profile = Project("profile", file("profile")).dependsOn(build, notification, common)

lazy val notification = Project("notification", file("notification")).dependsOn(build, common)

lazy val main = Project("main", file("main")).dependsOn(build, exercise, profile, notification, common)

lazy val root = (project in file(".")).aggregate(build, main, exercise, profile, notification, common)

mainClass in run := Some("com.eigengo.lift.LiftMain")

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
