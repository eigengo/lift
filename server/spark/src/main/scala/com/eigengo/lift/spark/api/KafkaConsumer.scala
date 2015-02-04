package com.eigengo.lift.spark.api

import com.typesafe.config.Config

trait KafkaConsumer {

  def kafkaConfig: Config

  def consume() = {
    println("BOOM")
  }
}
