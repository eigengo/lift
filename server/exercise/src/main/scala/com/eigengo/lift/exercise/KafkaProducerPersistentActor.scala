package com.eigengo.lift.exercise

import akka.actor.ActorRef
import akka.persistence.PersistentActor

/**
 * Gives ability to publish messages to Kafka actor
 */
abstract class KafkaProducerPersistentActor extends PersistentActor {
  val kafka: ActorRef

  /**
   * Persists message and at the same time produces it to kafka broker
   * Same intefrace as persist
   *
   * @param event
   * @param handler
   * @tparam A
   * @return
   */
  final def persistAndProduceToKafka[A](event: A)(handler: A ⇒ Unit): Unit = {
    persist(event)(handler)
    kafka ! event
  }

  private def kafkaProduceReceive(): Receive = new Receive {
    override def isDefinedAt(event: Any) = {
      kafka ! event
      false
    }

    override def apply(v1: Any): Unit = ???
  }

  /**
   * Wrapper for receive
   * All messages in the inner receive go through this one first
   * They are never handled, but as side effect sent to kafka
   * @param receive inner receive
   * @return concatenated receive
   */
  protected def withKafka(receive: Receive): Receive = kafkaProduceReceive.orElse(receive)
}
