package com.eigengo.lift.spark.jobs

import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}

import java.net._

case class PrintCassandraEvents() extends Batch[Int, Unit] {

  override def name: String = "PrintCassandra"

  override def execute(master: String, config: Config, params: Int): Either[String, Unit] = {

    println("SPARK DRIVER HOST:")
    println(InetAddress.getLocalHost.getHostAddress)

    val sc = new SparkContext(new SparkConf()
      .setAppName(name)
      .setMaster("local")
      .set("spark.cassandra.connection.host", "127.0.0.1")
      .set("spark.cassandra.journal.keyspace", "akka") // optional, defaults to "akka"
      .set("spark.cassandra.journal.table", "messages")
      .set("spark.driver.host", InetAddress.getLocalHost.getHostAddress)
      .set("spark.driver.port", "9001")) // optional, defaults to "messages"

    sc.eventTable().cache().collect().foreach(println)

    val sc = new SparkContext(new SparkConf()
      .setAppName(name)

      .setMaster(master))

    sc.parallelize(1 to params).map(_ + 1).foreach(println)

    sc.stop()

    Right()
  }
}
