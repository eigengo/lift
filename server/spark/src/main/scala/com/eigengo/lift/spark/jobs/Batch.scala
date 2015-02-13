package com.eigengo.lift.spark.jobs

import com.typesafe.config.Config

trait Batch[P, R] extends App {

  /**
   * Could help to have compatibility with submit job scripts
   */
  //override def main(args: Array[String]) = execute()

  def name: String

  def execute(master: String, config: Config, params: P): Either[String, R]
}