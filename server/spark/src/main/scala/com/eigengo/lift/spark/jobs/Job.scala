package com.eigengo.lift.spark.jobs

import scala.reflect.ClassTag

object Job {
  def apply[J <: Batch[_, _]: ClassTag]() = {
    implicitly[ClassTag[J]].runtimeClass.newInstance().asInstanceOf[J]
  }
}
