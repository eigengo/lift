/*
package com.eigengo.lift.spark

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.{ BootedNode, MicroserviceProps }
import com.typesafe.config.Config

/**
 * Entry point for Spark jobs executor service
 */
object SparkApp extends MicroserviceApp(MicroserviceProps("Spark")) {
  override def boot(config: Config)(implicit system: ActorSystem, cluster: Cluster): BootedNode = {
    val spark = SparkBoot.boot(config)

    spark
  }
}
*/
