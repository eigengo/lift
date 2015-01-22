package com.eigengo.lift.spark

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils

object KafkaStreamPrinter extends App {

  override def main(args: Array[String]): Unit = {
    val ssc = new StreamingContext(
      new SparkConf()
        .setAppName("Kafka Stream Printer")
        .setMaster("spark://cakes-mbp:7077"), Duration(1000))



    val numDStreams = 5
    val topics = Map("zerg.hydra" -> 1)
    val groupId = "testGroup.id"
    val zkQuorum = "123:2181"

    val kafkaDStreams = (1 to numDStreams).map { _ =>
      KafkaUtils.createStream(ssc, zkQuorum, groupId, topics, StorageLevel.NONE)
    }
  }
}
