import Dependencies._

Build.Settings.project

name := "lift-notification-link"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.testkit % "test",
  spray.testkit % "test"
)
