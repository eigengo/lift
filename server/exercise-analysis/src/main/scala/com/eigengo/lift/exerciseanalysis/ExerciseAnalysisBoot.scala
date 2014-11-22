package com.eigengo.lift.exerciseanalysis

import akka.actor.{Props, ActorSystem}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkEnv, SparkConf, SparkContext}

case class ExerciseAnalysisBoot()

object ExerciseAnalysisBoot {

  def boot(implicit system: ActorSystem): ExerciseAnalysisBoot = {
    val driverPort = 7777
    val driverHost = "localhost"
    val conf = new SparkConf(false) // skip loading external settings
      .setMaster("local[*]") // run locally with enough threads
      .setAppName("Spark Streaming with Scala and Akka") // name in Spark web UI
      .set("spark.logConf", "true")
      .set("spark.driver.port", s"$driverPort")
      .set("spark.driver.host", s"$driverHost")
      .set("spark.akka.logLifecycleEvents", "true")
    val ssc = new StreamingContext(conf, Seconds(1))
    

    ExerciseAnalysisBoot()
  }


}