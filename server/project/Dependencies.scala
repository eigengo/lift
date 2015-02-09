import sbt._
import Keys._

object Dependencies {

  object akka {
    val version = "2.3.8"
    // Core Akka
    val actor                 = "com.typesafe.akka"      %% "akka-actor"                    % version
    val cluster               = "com.typesafe.akka"      %% "akka-cluster"                  % version
    val contrib               = "com.typesafe.akka"      %% "akka-contrib"                  % version intransitive()
    val persistence           = "com.typesafe.akka"      %% "akka-persistence-experimental" % version intransitive()
    val persistence_cassandra = "com.github.krasserm"    %% "akka-persistence-cassandra"    % "0.3.4" intransitive()

    object streams {
      val version = "1.0-M2"

      val core      = "com.typesafe.akka" %% "akka-stream-experimental"         % version
      val http      = "com.typesafe.akka" %% "akka-http-experimental"           % version
      val http_core = "com.typesafe.akka" %% "akka-http-core-experimental"      % version

      // FIXME: currently this is an empty dependency - as soon as this situation changes, we need to ensure manually added files under `akka.stream.testkit.*` are deleted!
      val testkit   = "com.typesafe.akka" %% "akka-stream-testkit-experimental" % version
    }

    val leveldb               = "org.iq80.leveldb"        % "leveldb"                       % "0.7"

    val testkit               = "com.typesafe.akka"      %% "akka-testkit"                  % version
  }

  object spray {
    val version = "1.3.2"

    val httpx   = "io.spray" %% "spray-httpx"              % version
    val can     = "io.spray" %% "spray-can"                % version
    val routing = "io.spray" %% "spray-routing-shapeless2" % version
    val client  = "io.spray" %% "spray-client"             % version

    val testkit = "io.spray" %% "spray-testkit"            % version
  }

  object kafka {
    val version = "0.8.2-beta"
    val kafka   = ("org.apache.kafka" %% "kafka" % version)
      .exclude("org.slf4j", "slf4j-simple")
      .exclude("org.slf4j","slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
  }

  object scalaz {
    val core = "org.scalaz" %% "scalaz-core" % "7.1.0"
  }

  object json4s {
    val native  = "org.json4s" %% "json4s-native" % "3.2.11"
    val jackson = "org.json4s" %% "json4s-jackson" % "3.2.11"
  }

  object scalanlp {
    val version = "0.10"

    val breeze  = "org.scalanlp" %% "breeze"         % version
    val natives = "org.scalanlp" %% "breeze-natives" % version
    val nak     = "org.scalanlp" %% "nak"            % "1.3" exclude("org.scalanlp", "breeze_2.11") exclude("org.scalanlp", "breeze-natives_2.11")
  }

  object spark {
    val version = "1.2.0"

    val core = ("org.apache.spark" %% "spark-core" % version)
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.eclipse.jetty.orbit", "javax.transaction")
      .exclude("org.eclipse.jetty.orbit", "javax.mail")
      .exclude("org.eclipse.jetty.orbit", "javax.mail.glassfish")
      .exclude("org.eclipse.jetty.orbit", "javax.activation")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("commons-logging", "commons-logging")
      .exclude("com.esotericsoftware.minlog", "minlog")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.apache.hadoop", "hadoop-yarn-api")
    val mllib = ("org.apache.spark" %% "spark-mllib" % version)
      .exclude("org.slf4j", "slf4j-api")
    val streaming = "org.apache.spark" %% "spark-streaming" % version
    val streamingKafka = ("org.apache.spark" %% "spark-streaming-kafka" % version)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-collections", "commons-collections")
      .exclude("com.esotericsoftware.minlog", "minlog")
  }

  object akkaAnalytics {
    val version = "0.2"

    val cassandra = ("com.github.krasserm" % "akka-analytics-cassandra_2.10" % version)
      .exclude("com.typesafe.akka", "akka-actor_2.10")
      .exclude("com.esotericsoftware.minlog", "minlog")
      .exclude("commons-beanutils", "commons-beanutils-core")
      .exclude("commons-collections", "commons-collections")
      .exclude("org.slf4j", "jcl-over-slf4j")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.apache.spark", "spark-core_2.10")
      .exclude("commons-logging", "commons-logging")
  }

  val typesafeConfig   = "com.typesafe" % "config" % "1.2.1"

  val scodec_bits      = "org.typelevel"    %% "scodec-bits"  % "1.0.4"
  val parboiled        = "org.parboiled"    %% "parboiled"    % "2.0.1"

  // Scala reflect
  val scala_reflect    = "org.scala-lang"   % "scala-reflect" % "2.11.4"

  // Apple push notifications
  val apns             = "com.notnoop.apns"  % "apns"         % "0.1.6"
  val slf4j_simple     = "org.slf4j"         % "slf4j-simple" % "1.6.1"
 
  // Datastax Cassandra Client
  val cassandra_driver = "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.1" exclude("io.netty", "netty")

  // Testing
  val scalatest        = "org.scalatest"    %% "scalatest"    % "2.2.1"
  val scalacheck       = "org.scalacheck"   %% "scalacheck"   % "1.12.1"
}
