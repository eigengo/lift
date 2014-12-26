import Dependencies._

Build.Settings.project

name := "profile"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  spray.routing,
  scalaz.core,
  scala_reflect,
  akka.persistence_cassandra,
  akka.testkit % "test",
  spray.testkit % "test"
)
