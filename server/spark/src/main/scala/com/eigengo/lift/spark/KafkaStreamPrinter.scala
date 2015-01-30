package com.eigengo.lift.spark

import com.eigengo.lift.spark.SparkServiceProtocol.Analytics
import com.typesafe.config.{ConfigFactory, Config}
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{ Time, Duration, StreamingContext }
import org.apache.spark.streaming.kafka.KafkaUtils

object KafkaStreamPrinter extends App {

  private def kafkaConsumerConfig(config: Config) = config.getConfig("kafka.consumer")

  override def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val ssc = new StreamingContext(
      new SparkConf()
        .setAppName("Kafka Stream Printer")
        .setMaster(config.getString("spark.master")), Duration(1000))

    //TODO: Configurabla parallelism
    val numDStreams = 5

    //val kafkaDStreams = (1 to numDStreams).map { _ =>
    KafkaUtils.createStream(
      ssc,
      kafkaConsumerConfig(config).getString("zkConnect"),
      kafkaConsumerConfig(config).getString("groupId"),
      Map(kafkaConsumerConfig(config).getString("topic") -> 1),
      StorageLevel.NONE)
        //.foreachRDD(x => println(x))
        .print()
    //}   .

    ssc.start()
    //ssc.stop()
  }
}