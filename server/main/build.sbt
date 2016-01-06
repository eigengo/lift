import Config._
import Dependencies._

Build.Settings.project

name := "main"

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

import DockerKeys._
import sbtdocker.ImageName
import sbtdocker.mutable.Dockerfile

dockerSettings

// Define a Dockerfile for:
//   - `LiftLocalApp`
//   - `LiftContainerApp`

// TODO: work out how to do multiple docker container builds!
//inConfig(LiftLocalApp)(Seq(
//  mainClass in assembly := Some("com.eigengo.lift.LiftLocalApp")
//))

//inConfig(LiftContainerApp)(Seq(
//  mainClass in assembly := Some("com.eigengo.lift.LiftContainerApp")
//))
mainClass in assembly := Some("com.eigengo.lift.LiftContainerApp")

docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")
    val f = new File(s"${Path.userHome.absolutePath}/.ios")
    if (f.exists) add(f, "/root/.ios")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

imageName in docker := {
  ImageName(
    namespace = Some("janm399"),
    repository = "lift",
    tag = Some(s"${name.value}-production"))
}
