package com.eigengo.lift.analysis.exercise.ld

import java.util.Properties
import java.util.concurrent.Executors

import com.eigengo.lift.analysis.exercise.rt.ExerciseClassificationProtocol.{Payload, Train, ExerciseClassificationRequest}
import com.eigengo.lift.analysis.exercise.rt.{Exercise, MessageEncoder, MessageDecoder, JavaSerializationCodecs}
import kafka.consumer.{ConsumerConfig, Consumer}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import kafka.serializer.StringDecoder

import scala.io.Source
import scala.util.Random

object AccelerometerDataLocalLoader extends JavaSerializationCodecs {

  val encoder = implicitly[MessageEncoder[ExerciseClassificationRequest]]
  def messagesPerSec = 1 + Random.nextInt(10)
  def wordsPerMessage = 10 + Random.nextInt(100)

  val topic = "accelerometer-data"
  val producer = {
    val brokers = "192.168.59.103:9092"
    val props = new Properties()
    props.put("metadata.broker.list", brokers)

    val config = new ProducerConfig(props)
    new Producer[String, Payload](config)
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

  def load[U](name: String)(f: Payload => ExerciseClassificationRequest): Array[Byte] = {
    val msg = f(Source.fromInputStream(getClass.getResourceAsStream(name)).map(_.toByte).toArray)
    encoder.encode(msg).toOption.get
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
      val msg = load("/arms1.adv1")(Train(_, Exercise("Arms", 1.0)))
      producer.send(new KeyedMessage[String, Payload](topic, msg))
      Thread.sleep(100)
    }
  }

}
