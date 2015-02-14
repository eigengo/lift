package com.eigengo.lift.spark.jobs

import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}
import akka.analytics.cassandra._


class exerciseSuggestions extends Batch[Int, Int] {
  /**
   * Could help to have compatibility with submit job scripts
   */
  override def name: String = "exerciseSuggestions"

  override def execute(master: String, config: Config, params: Int): Either[String, Int] = {
    val sc = new SparkContext(new SparkConf()
      .setAppName(name)
      .set("spark.cassandra.connection.host", config.getString("cassandra.host"))
      .setMaster(master))

    sc.eventTable().map(x => s"Key: ${x._1} value: ${x._2}").foreach(println)

    Right(5)
  }
}
