package com.eigengo.lift.analysis.exercise.ld

import java.util.Properties

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

import scala.util.Random

object AccelerometerDataLocalLoader {

  val brokers = "192.168.59.103:9092"
  val topic = "accelerometer-data"
  def messagesPerSec = 1 + Random.nextInt(10)
  def wordsPerMessage = 10 + Random.nextInt(100)

  def main(args: Array[String]) {
    // Zookeper connection properties
    val props = new Properties()
    props.put("metadata.broker.list", brokers)
    props.put("serializer.class", "kafka.serializer.StringEncoder")

    val config = new ProducerConfig(props)
    val producer = new Producer[String, String](config)

    // Send some messages
    while(true) {
      val messages = (1 to messagesPerSec).map { messageNum =>
        val str = (1 to wordsPerMessage).map(x => Random.nextString(30))
          .mkString(" ")

        new KeyedMessage[String, String](topic, str)
      }.toArray

      producer.send(messages: _*)
      Thread.sleep(100)
    }
  }

}
