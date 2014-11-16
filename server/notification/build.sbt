import com.eigengo.lift.build.Dependencies._
import com.eigengo.lift.build._

Build.Settings.project

name := "lift-notification"

libraryDependencies ++= Seq(
  akka.actor,
  akka.cluster,
  akka.contrib,
  apns,
  akka.testkit % "test",
  spray.testkit % "test"
)
