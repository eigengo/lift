import sbt._
import Keys._

object Dependencies {

  object kafka {
    val version = "0.8.1.1"
    val kafka = "org.apache.kafka" %% "kafka"  % version exclude("log4j", "log4j") exclude("org.slf4j","slf4j-log4j12")
  }

  object spark {
    val version = "1.1.0"

    val core = "org.apache.spark" %% "spark-core" % version
    val streaming = "org.apache.spark" %% "spark-streaming" % version
    val streaming_kafka = "org.apache.spark" %% "spark-streaming-kafka" % version
    val mllib = "org.apache.spark" %% "spark-mllib" % version
  }

  object scalaz {
    val core = "org.scalaz" %% "scalaz-core"  % "7.1.0"
  }

  object json4s {
    val native = "org.json4s" %% "json4s-native" % "3.2.10"
    val jackson = "org.json4s" %% "json4s-jackson" % "3.2.10"
  }

  val slf4j_simple = "org.slf4j"         % "slf4j-simple" % "1.6.1"

  // Testing
  val scalatest    = "org.scalatest"    %% "scalatest"    % "2.2.1"
  val scalacheck   = "org.scalacheck"   %% "scalacheck"   % "1.11.6"

}
