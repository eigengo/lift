import com.eigengo.lift.build.Dependencies._
import com.eigengo.lift.build._

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
