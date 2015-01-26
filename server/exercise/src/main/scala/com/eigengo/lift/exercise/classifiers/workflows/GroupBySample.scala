package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.stage.{TerminationDirective, Directive, Context, PushStage}
import scala.collection.mutable

/**
 * Streaming stage that buffers events (the sample window). Once the buffer has filled, a strategy function is applied
 * to determine the elements to be emitted. Emitted values are (typically) either a grouping of the elements making up
 * the sample or single sample elements.
 *
 * @param size     size of the internal buffer and so the sampling window size
 * @param strategy when buffer is full, sample window strategy that determines the elements to be emitted
 */
class GroupBySample[A] private (size: Int, strategy: List[A] => GroupBySample.GroupValue[A]) extends PushStage[A, GroupBySample.GroupValue[A]] {
  require(size > 0)

  import GroupBySample._

  private val buffer = mutable.Queue[A]()

  private def applyStrategy(ctx: Context[GroupValue[A]]): Directive = {
    strategy(buffer.toList) match {
      case value @ BlobValue(sample) =>
        // Aggregated sample to be emitted
        buffer.drop(sample.length)
        ctx.push(value)

      case value: SingleValue[A] =>
        // Single value sample is to be emitted
        buffer.dequeue()
        ctx.push(value)
    }
  }

  override def onPush(elem: A, ctx: Context[GroupValue[A]]): Directive = {
    if (!ctx.isFinishing) {
      if (buffer.length == size) {
        // Buffer is full, so apply our strategy to determine emit behaviour
        applyStrategy(ctx)
      } else {
        // Buffer is not yet full, so keep consuming from our upstream
        buffer.enqueue(elem)
        ctx.pull()
      }
    } else {
      // Streaming stage is shutting down, so we ensure that all buffer elements are flushed prior to finishing
      if (buffer.isEmpty) {
        // Buffer is empty, so we simply finish
        ctx.finish()
      } else if (buffer.length == 1) {
        // One element in the buffer, so push it and finish
        ctx.pushAndFinish(SingleValue(buffer.dequeue()))
      } else if (buffer.length == size) {
        // Buffer is full, so apply our strategy to determine emit behaviour
        applyStrategy(ctx)
      } else {
        // Multiple elements are in the buffer (and it isn't full), so reduce its size and eventually we will finish
        ctx.push(SingleValue(buffer.dequeue()))
      }
    }
  }

  override def onUpstreamFinish(ctx: Context[GroupValue[A]]): TerminationDirective = {
    ctx.absorbTermination()
  }
}

object GroupBySample {

  /**
   * Trait that allows data streams to be grouped into common values (c.f. `BlobValue`). Data values that can not be
   * grouped are "marked" with `SingleValue`.
   */
  sealed trait GroupValue[A]
  case class BlobValue[A](value: List[A]) extends GroupValue[A]
  case class SingleValue[A](value: A) extends GroupValue[A]

  def apply[A](size: Int)(strategy: List[A] => GroupValue[A]) = {
    new GroupBySample[A](size, strategy)
  }

}
