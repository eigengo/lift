import sbt._
import Keys._

name := "lift"

//Common code, but not protocols
lazy val common = project.in(file("common"))

//Exercise
lazy val exercise = project.in(file("exercise")).dependsOn(profileProtocol, profile, common)

//User profiles
lazy val profile = project.in(file("profile")).dependsOn(profileProtocol, common)
lazy val profileProtocol = project.in(file("profile-protocol")).dependsOn(common)

//Main 
lazy val main = project.in(file("main")).dependsOn(exercise, profile, common)

//The main aggregate
lazy val root = (project in file(".")).aggregate(main, exercise, profile, common)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
