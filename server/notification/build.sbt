import Dependencies._

Build.Settings.project

name := "notification"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence_cassandra,
  apns,
  akka.testkit % "test",
  spray.testkit % "test"
)

