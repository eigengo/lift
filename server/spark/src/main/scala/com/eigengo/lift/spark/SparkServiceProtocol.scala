package com.eigengo.lift.spark

object SparkServiceProtocol {
  sealed trait SparkServiceProtocol
  case class Stream(data: String) extends SparkServiceProtocol
  case class Batch(data: String) extends SparkServiceProtocol

  sealed trait SparServiceResponse
  case class Analytics(data: String) extends SparServiceResponse
}
