/**
 * Based on https://github.com/kevinschmidt/docker-spark
 *
 * If made dependent on common results on large amount of new dependency conflicts
 * Including akka etc.
 * 
 * If Spark version 1.2.0 used again large amount of new dependency conflicts
 *
 * Potentially upgradeable to Scala 2.11
 */

import Dependencies._

Build.Settings.project

name := "spark"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  slf4j_simple,
  akkaAnalytics.cassandra,
  spark.core,
  spark.mllib
  //spark.streaming,
  //spark.streamingKafka
)

import DockerKeys._
import sbtdocker.ImageName
import sbtdocker.mutable.Dockerfile

dockerSettings

mainClass in assembly := Some("com.eigengo.lift.spark.Spark")

docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("dockerfile/java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

imageName in docker := {
  ImageName(
    namespace = Some("janm399"),
    repository = "lift",
    tag = Some(name.value))
}