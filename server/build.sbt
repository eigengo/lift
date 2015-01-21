import sbt._
import Keys._

name := "domain"

//Common code, but not protocols
lazy val common = project.in(file("common")).dependsOn(contrib)

//Kafka integration
lazy val kafka = project.in(file("kafka")).dependsOn(common)

//Exercise
lazy val exercise = project.in(file("exercise")).dependsOn(notificationProtocol, profileProtocol, common, kafka)

//User profiles
lazy val profile = project.in(file("profile")).dependsOn(profileProtocol, common)
lazy val profileProtocol = project.in(file("profile-protocol")).dependsOn(common, notificationProtocol)

//Notifications
lazy val notification = project.in(file("notification")).dependsOn(common, notificationProtocol)
lazy val notificationProtocol = project.in(file("notification-protocol")).dependsOn(common)

//Main
lazy val main = project.in(file("main")).dependsOn(exercise, profile, notification, common)

//The unified API adapter
lazy val adapter = project.in(file("adapter")).dependsOn(common)

//The cluster config
lazy val contrib = project.in(file("contrib"))

//The main aggregate
lazy val root = (project in file(".")).aggregate(main, exercise, profile, notification, common, adapter, kafka)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
