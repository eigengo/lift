package com.eigengo.lift.spark.jobs

import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}

import scalaz.{\/-, \/}

case class PrintSequence() extends Batch[Int, Unit] {

  override def name: String = "PrintSequence"

  override def execute(master: String, config: Config, params: Int): \/[String, Unit] = {
    val sc = new SparkContext(new SparkConf()
      .setAppName(name)
      .set("spark.driver.port", "9001")
      .setMaster(master))

    sc.parallelize(1 to params).map(_ + 1).foreach(println)

    sc.stop()

    \/-()
  }
}
