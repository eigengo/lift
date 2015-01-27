package com.eigengo.lift.spark

import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils

object KafkaStreamPrinter extends App {

  override def main(args: Array[String]): Unit = {
    val ssc = new StreamingContext(
      new SparkConf()
        .setAppName("Kafka Stream Printer")
        .setMaster("spark://90e2f07d86e9:7077"), Duration(1000))

    val config = ConfigFactory.load()

    //TODO: Configurabla parallelism
    val numDStreams = 5

    val kafkaConsumerConfig = config.getConfig("kafka.consumer")

    val kafkaDStreams = (1 to numDStreams).map { _ =>
      KafkaUtils.createStream(
        ssc,
        kafkaConsumerConfig.getString("zkConnect"),
        kafkaConsumerConfig.getString("groupId"),
        Map(kafkaConsumerConfig.getString("topic") -> 1),
        StorageLevel.NONE)
    }
  }
}