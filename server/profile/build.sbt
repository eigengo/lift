import Dependencies._

Build.Settings.project

name := "profile"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  spray.routing,
  scalaz.core,
  akka.persistence_cassandra,
  akka.testkit % "test",
  spray.testkit % "test"
)
