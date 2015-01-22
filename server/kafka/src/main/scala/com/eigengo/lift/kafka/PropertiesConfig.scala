package com.eigengo.lift.kafka

import com.typesafe.config.Config
import scala.collection.JavaConverters.asScalaSetConverter
import scala.language.implicitConversions

object PropertiesConfig {
  implicit def configToPropertiesConfig(config: Config): PropertiesConfig = PropertiesConfig(config)
}

case class PropertiesConfig(config: Config) {
  def properties: java.util.Properties = {
    val entries =
      config.entrySet.asScala.map { entry =>
        val key = entry.getKey
        val value = config.getString(key)
        (key, value)
      }

    val properties = new java.util.Properties()
    entries.foreach { case (key, value) => properties.put(key, value) }

    properties
  }
}
