package com.eigengo.lift.spark.api

import com.typesafe.config.Config

/**
 * Created by martinzapletal on 03/02/15.
 */
trait KafkaConsumer {

  def kafkaConfig: Config
}
