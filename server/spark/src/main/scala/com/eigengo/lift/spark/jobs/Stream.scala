package com.eigengo.lift.spark.jobs

import com.typesafe.config.Config

import scalaz.\/

trait Stream[T] extends App {
  def name: String

  def execute(master: String, config: Config): \/[String, T]
}
