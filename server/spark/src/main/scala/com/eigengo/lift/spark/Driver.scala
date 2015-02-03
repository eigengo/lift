package com.eigengo.lift.spark

import com.eigengo.lift.spark.jobs.Batch
import com.typesafe.config.Config
import org.apache.log4j.Logger

import scala.reflect.ClassTag
import scalaz.\/

trait Driver {

  def config: Config

  def master: String

  private val logger = Logger.getLogger(classOf[Driver])

  def submit[P, R](job: Batch[P, R], jobParam: P): \/[String, R] = {

    logger.info(s"Executing job ${job.name} on master $master")
    val result = job.execute(master, config, jobParam)
    logger.info(s"Job ${job.name} finished with result $result")

    result
  }

  def submit[T](job: Stream[T]): \/[String, T] = ???
}
