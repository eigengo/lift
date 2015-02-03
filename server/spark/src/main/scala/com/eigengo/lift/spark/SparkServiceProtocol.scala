package com.eigengo.lift.spark

object SparkServiceProtocol {
  sealed trait SparkServiceProtocol
  case class Stream[T](job: T) extends SparkServiceProtocol
  case class Batch[T](job: T) extends SparkServiceProtocol

  sealed trait SparServiceResponse
  case class Analytics(data: String) extends SparServiceResponse
}
