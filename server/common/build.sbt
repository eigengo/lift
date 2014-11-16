import com.eigengo.lift.build.Dependencies._
import com.eigengo.lift.build.Build._

Settings.project

name := "lift-common"

libraryDependencies ++= Seq(
  // Core Akka
  akka.actor,
  akka.cluster,
  akka.contrib,
  akka.persistence,
  spray.routing,
  json4s.native,
  scodec_bits
)
