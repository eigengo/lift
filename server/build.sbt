import sbt._
import Keys._

name := "lift"

//Build settings. TODO: Set up proper resolvers so that one doesn't have to run
//sbt publish-local in project build
// lazy val build = project.in(file("build"))

//Common code, but not protocols
lazy val common = project.in(file("common"))

//Exercise
lazy val exercise = project.in(file("exercise")).dependsOn(notificationProtocol, profileProtocol, common)

//User profiles
lazy val profile = project.in(file("profile")).dependsOn(profileProtocol, notificationProtocol, common)

lazy val profileProtocol = project.in(file("profile-protocol")).dependsOn(common)

//Notifications
lazy val notification = project.in(file("notification")).dependsOn(common, notificationProtocol, profileProtocol)

lazy val notificationProtocol = project.in(file("notification-protocol")).dependsOn(common)

//Main 
lazy val main = project.in(file("main")).dependsOn(exercise, profile, notification, common)

lazy val root = (project in file(".")).aggregate(main, exercise, profile, notification, notificationProtocol, common)

mainClass in run := Some("com.eigengo.lift.LiftMain")

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
