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
    //add(new File(s"${Path.userHome.absolutePath}/.ios"), "/root/.ios")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
