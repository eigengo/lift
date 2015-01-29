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

version := "1.0"

libraryDependencies ++= Seq(
  spark.core,
  spark.mllib,
  spark.streaming,
  spark.streamingKafka
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList("javax", "transaction", xs @ _*)     => MergeStrategy.first
  case PathList("javax", "mail", xs @ _*)     => MergeStrategy.first
  case PathList("javax", "activation", xs @ _*)     => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt"     => MergeStrategy.discard
  case x => old(x)
}}

import DockerKeys._
import sbtdocker.ImageName
import sbtdocker.mutable.Dockerfile

dockerSettings

mainClass in assembly := Some("com.eigengo.lift.SparkApp")

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