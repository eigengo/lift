package com.eigengo.lift.spark.jobs

import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}

import java.net._

case class PrintSequence() extends Batch[Int, Unit] {

  override def name: String = "PrintSequence"

  override def execute(master: String, config: Config, params: Int): Either[String, Unit] = {

    println("SPARK DRIVER HOST:")
    println(InetAddress.getLocalHost.getHostAddress)

    /*val sc = new SparkContext(new SparkConf()
      .setAppName(name)
      .setMaster(master))

    sc.parallelize(1 to params).map(_ + 1).foreach(println)

    sc.stop()*/

    Right()
  }
}
