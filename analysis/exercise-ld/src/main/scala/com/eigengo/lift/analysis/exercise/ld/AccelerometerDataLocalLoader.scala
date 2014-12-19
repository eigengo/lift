package com.eigengo.lift.analysis.exercise.ld

import java.util.Properties

import kafka.consumer.{ConsumerConfig, Consumer}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

import scala.util.Random

object AccelerometerDataLocalLoader {

  def messagesPerSec = 1 + Random.nextInt(10)
  def wordsPerMessage = 10 + Random.nextInt(100)

  val topic = "accelerometer-data"
  val producer = {
    val brokers = "192.168.59.103:9092"
    val props = new Properties()
    props.put("metadata.broker.list", brokers)
    props.put("serializer.class", "kafka.serializer.StringEncoder")

    val config = new ProducerConfig(props)
    new Producer[String, String](config)
  }
  val consumer = {
    val zkQuorum = "192.168.59.103"
    val group = "lift"
    val props = new Properties()
    props.put("zookeeper.connect", zkQuorum)
    props.put("group.id", group)
    props.put("zookeeper.connection.timeout.ms", "10000")

    val config = new ConsumerConfig(props)
    Consumer.create(config)
  }

  def main(args: Array[String]) {
    // Send some messages
    while(true) {
      val messages = (1 to messagesPerSec).map { messageNum =>
        val str = (1 to wordsPerMessage).map(x => Random.nextString(30)).mkString(" ")

        new KeyedMessage[String, String](topic, str)
      }.toArray

      producer.send(messages: _*)
      Thread.sleep(100)
    }
  }

}
