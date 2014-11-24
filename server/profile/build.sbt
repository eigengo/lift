import Dependencies._

Build.Settings.project

name := "lift-profile"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  spray.routing,
  scalaz.core,
  akka.testkit % "test",
  spray.testkit % "test"
)

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
    expose(12553)
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
