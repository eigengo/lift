import sbt._
import Keys._

name := "analysis"

//Exercise: the common code (AccelerometerData decoder, ...)
lazy val exercise = project.in(file("exercise"))

//"Real-time" analysis code
//The Spark Streaming & Kafka runner
lazy val exerciseRt = project.in(file("exercise-rt")).dependsOn(exercise, exerciseRtProtocol)
lazy val exerciseRtProtocol = project.in(file("exercise-rt-protocol")).dependsOn(exercise)

//"Deep learning" analysis code
lazy val exerciseDl = project.in(file("exercise-dl")).dependsOn(exercise)

//Local test diver
lazy val exerciseLd = project.in(file("exercise-ld")).dependsOn(exerciseRtProtocol)

//The main aggregate
lazy val root = (project in file(".")).aggregate(exerciseRt, exerciseDl, exerciseLd)

fork in Test := false

fork in IntegrationTest := false

parallelExecution in Test := false

publishLocal := {}

publish := {}
