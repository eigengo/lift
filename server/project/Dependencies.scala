import sbt._
import Keys._

object Dependencies {

  object akka {
    val version = "2.3.6"
    // Core Akka
    val actor       = "com.typesafe.akka"  %% "akka-actor"                    % version
    val cluster     = "com.typesafe.akka"  %% "akka-cluster"                  % version
    val contrib     = "com.typesafe.akka"  %% "akka-contrib"                  % version
    val persistence = "com.typesafe.akka"  %% "akka-persistence-experimental" % version
    
    val testkit     = "com.typesafe.akka"  %% "akka-testkit"                  % version
  }

  object spray {
    val version = "1.3.2"

    val httpx   = "io.spray" %% "spray-httpx"   % version
    val can     = "io.spray" %% "spray-can"     % version
    val routing = "io.spray" %% "spray-routing" % version

    val testkit = "io.spray" %% "spray-testkit" % version
  }

  object kafka {
    val version = "0.8.2-beta"
    val kafka = "org.apache.kafka" %% "kafka"  % version exclude("log4j", "log4j") exclude("org.slf4j","slf4j-log4j12")
  }

  object spark {
    val version = "1.3.0-SNAPSHOT"

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

  val scodec_bits  = "org.typelevel"    %% "scodec-bits"  % "1.0.4"
  
  // Apple push notifications
  val apns         = "com.notnoop.apns"  % "apns"         % "0.1.6"
  val slf4j_simple = "org.slf4j"         % "slf4j-simple" % "1.6.1"

  // Testing
  val scalatest    = "org.scalatest"    %% "scalatest"    % "2.2.1"
  val scalacheck   = "org.scalacheck"   %% "scalacheck"   % "1.11.6"

}
