package com.eigengo.lift.analysis.exercise.ld

import java.util.Properties
import java.util.concurrent.Executors

import kafka.consumer.{ConsumerConfig, Consumer}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.StringDecoder

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
    props.put("zookeeper.session.timeout.ms", "400")
    props.put("zookeeper.sync.time.ms", "200")
    props.put("auto.commit.interval.ms", "1000")

    val config = new ConsumerConfig(props)
    Consumer.create(config)
  }

  def main(args: Array[String]) {
    // Send some messages
    val executor = Executors.newFixedThreadPool(10)
    val streams = consumer.createMessageStreams(Map("classified-exercise" → 1), keyDecoder = new StringDecoder(), valueDecoder = new StringDecoder())
    executor.submit(new Runnable {
      override def run(): Unit = {
        streams.foreach {
          case (key, stream) ⇒
            println(s"Consuming from $key at ${System.currentTimeMillis()}")
            stream.foreach { x ⇒
              x.iterator().foreach { mam ⇒
                println(mam.message())
              }
            }
            println(s"Done")
        }
      }
    })

    while(true) {
      val messages = (1 to messagesPerSec).map { messageNum =>
        val str = (1 to wordsPerMessage).map(x => Random.alphanumeric.head).mkString(" ")

        new KeyedMessage[String, String](topic, str)
      }.toArray

      producer.send(messages: _*)

      Thread.sleep(100)
    }
  }

}
