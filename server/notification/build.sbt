import Dependencies._

Build.Settings.project

name := "lift-notification"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence_cassandra,
  apns,
  akka.testkit % "test",
  spray.testkit % "test"
)

