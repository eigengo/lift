import Dependencies._

Build.Settings.project

name := "lift-analysis-exercise"

libraryDependencies ++= Seq(
  // Spark
  spark.core,
  spark.streaming,
  spark.streaming_kafka,
  // Kafka
  kafka.kafka,
  // Testing
  scalatest % "test",
  scalacheck % "test"
)

val sparkHome = settingKey[String]("Spark home directory")
sparkHome := "/usr/local/Cellar/apache-spark/1.1.1"

val sparkRun = taskKey[Unit]("Submit the spark job.")

sparkRun := {
  for {
    mainClass ← (selectMainClass in Compile).value
    fatJar    =  assembly.value
    cmd       =  s"${sparkHome.value}/bin/spark-submit --class $mainClass $fatJar"
    _ = println(cmd)
  } yield cmd!
}
