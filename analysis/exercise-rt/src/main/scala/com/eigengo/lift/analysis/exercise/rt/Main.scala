package com.eigengo.lift.analysis.exercise.rt

import java.util.Properties

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: KafkaWordCount <zkQuorum> <group> <topics> <numThreads>
 *   <zkQuorum> is a list of one or more zookeeper servers that make quorum
 *   <group> is the name of kafka consumer group
 *   <topics> is a list of one or more kafka topics to consume from
 *   <numThreads> is the number of threads the kafka consumer should use
 *
 * Example:
 *    `$ bin/run-example \
 *      org.apache.spark.examples.streaming.KafkaWordCount zoo01,zoo02,zoo03 \
 *      my-consumer-group topic1,topic2 1`
 */
object Main {

  val zkQuorum = "192.168.59.103"
  val group = "lift"
  val topicMap = Map("accelerometer-data" → 8)
  val kafkaParams = Map("zookeeper.connect" → zkQuorum, "group.id" → group)

  val producer = {
    val brokers = "192.168.59.103:9092"
    val props = new Properties()
    props.put("metadata.broker.list", brokers)
    props.put("serializer.class", "kafka.serializer.StringEncoder")

    val config = new ProducerConfig(props)
    new Producer[String, String](config)
  }

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("KafkaWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    ssc.checkpoint("checkpoint")

    /*
    val rawAccelerometerData = KafkaUtils.createStream[String, Array[Byte], StringDecoder, DefaultDecoder](ssc, kafkaParams, topicMap, StorageLevel.MEMORY_ONLY).map(_._2)
    val decodedAccelerometerData = rawAccelerometerData.flatMap(data ⇒ AccelerometerData.decodeAll(BitVector(data), Nil)._2)

    val words = rawAccelerometerData.flatMap(_.split(" "))
     */
    val topics = "accelerometer-data"
    val topicMap = topics.split(",").map((_, 8)).toMap
    val lines = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1L)).reduceByKeyAndWindow(_ + _, _ - _, Minutes(10), Seconds(2), 2)
    wordCounts.foreachRDD(_.foreach {
      case (word, count) ⇒
        val message = s"$word -> $count"
        producer.send(new KeyedMessage("classified-exercise", message))
        println(message)
    })

    ssc.start()
    ssc.awaitTermination()
  }

}
