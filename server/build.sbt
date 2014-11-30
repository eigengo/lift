import sbt._
import Keys._

name := "lift"

//Common code, but not protocols
lazy val common = project.in(file("common"))

//Exercise
lazy val exercise = project.in(file("exercise")).dependsOn(notificationProtocol, notificationLink, exerciseLink, profileProtocol, profileLink, common)
lazy val exerciseLink = project.in(file("exercise-link")).dependsOn(common)

//User profiles
lazy val profile = project.in(file("profile")).dependsOn(profileProtocol, profileLink, common)
lazy val profileProtocol = project.in(file("profile-protocol")).dependsOn(common)
lazy val profileLink = project.in(file("profile-link")).dependsOn(common)

//Notifications
lazy val notification = project.in(file("notification")).dependsOn(common, notificationLink, profileLink, notificationProtocol, profileProtocol)
lazy val notificationProtocol = project.in(file("notification-protocol")).dependsOn(common)
lazy val notificationLink = project.in(file("notification-link")).dependsOn(common)

//Main 
lazy val main = project.in(file("main")).dependsOn(exercise, profile, notification, common)

//The main aggregate
lazy val root = (project in file(".")).aggregate(main, exercise, profile, notification, notificationProtocol, common)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
