import Dependencies._

Build.Settings.project

name := "lift-main"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  akka.persistence_cassandra,
  akka.leveldb,
  // For REST API
  spray.httpx,
  spray.can,
  spray.routing,
  // Codec
  scodec_bits,
  scalaz.core,
  // Apple push notifications
  apns,
  slf4j_simple,
  // Testing
  scalatest % "test",
  scalacheck % "test",
  akka.testkit % "test",
  spray.testkit % "test"
)

/*
import DockerKeys._
import sbtdocker.mutable.Dockerfile

dockerSettings

// Define a Dockerfile
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("dockerfile/java")
    expose(12551, 12552, 12553, 12554)
    add(new File("/Users/janmachacek/.ios"), "/root/.ios")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
*/