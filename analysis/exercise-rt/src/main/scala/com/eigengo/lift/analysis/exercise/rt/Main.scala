package com.eigengo.lift.analysis.exercise.rt

import java.util.Properties

import _root_.kafka.serializer.{DefaultDecoder, StringDecoder}
import com.eigengo.lift.analysis.exercise.rt.ExerciseClassificationProtocol.ExerciseClassificationRequest
import _root_.kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import org.apache.spark.SparkConf
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.mllib.tree.configuration.Strategy
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import scalaz.\/-

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
object Main extends JavaSerializationCodecs {

  val decoder = implicitly[MessageDecoder[ExerciseClassificationRequest]]
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

    val rawAccelerometerData = KafkaUtils.createStream[String, Array[Byte], StringDecoder, DefaultDecoder](ssc, kafkaParams, topicMap, StorageLevel.MEMORY_ONLY).map(_._2)
    val decodedAccelerometerData = rawAccelerometerData
      .map { msg ⇒
        for {
          r  ← decoder.decode(msg)
          ad ← r.decodedPayload[AccelerometerData]
        } yield (r, ad)
      }                       // decode the message into our class
      .filter(_.isRight)      // keep only successes
      .map {
        case \/-((req, ad)) ⇒ LabeledPoint(0, Vectors.dense(ad.values.map(_.x.toDouble).toArray))
      }
      .foreachRDD { rdd ⇒
        val dtm = DecisionTree.train(rdd, Strategy.defaultStrategy("Classification"))
        println(dtm)
        producer.send(new KeyedMessage("classified-exercise", "Some such"))
      }

    ssc.start()
    ssc.awaitTermination()
  }

}
