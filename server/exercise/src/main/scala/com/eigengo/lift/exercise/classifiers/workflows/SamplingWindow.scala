package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.stage.{TerminationDirective, Directive, Context, PushPullStage}
import scala.collection.mutable

/**
 * Streaming stage that buffers events (the sample window). Once the buffer has filled, a callback  action is executed
 * for each new sample message that we receive.
 *
 * @param size   size of the internal buffer and so the sampling window size
 * @param action when buffer is full, action to be performed for each sample that we subsequently receive
 */
class SamplingWindow[A] private (size: Int, action: List[A] => Unit) extends PushPullStage[A, A] {
  require(size > 0)

  private val buffer = mutable.Queue[A]()

  override def onPush(elem: A, ctx: Context[A]): Directive = {
    if (buffer.length == size) {
      // Buffer is full, so perform action callback
      val emit = buffer.dequeue()
      buffer.enqueue(elem)
      action(buffer.toList)
      // Allow old events to propagate downstream
      ctx.push(emit)
    } else {
      // Buffer is not yet full, so keep consuming from our upstream
      buffer.enqueue(elem)
      if (buffer.length == size) {
        // Buffer is full, so perform action callback
        action(buffer.toList)
      }
      ctx.pull()
    }
  }

  override def onPull(ctx: Context[A]): Directive = {
    if (ctx.isFinishing) {
      // Streaming stage is shutting down, so we ensure that all buffer elements are flushed prior to finishing
      if (buffer.isEmpty) {
        // Buffer is empty, so we simply finish
        ctx.finish()
      } else if (buffer.length == 1) {
        // One element in the buffer, so push it and finish
        ctx.pushAndFinish(buffer.dequeue())
      } else {
        // Multiple elements are in the buffer, so reduce its size and eventually we will finish
        ctx.push(buffer.dequeue())
      }
    } else {
      ctx.pull()
    }
  }

  override def onUpstreamFinish(ctx: Context[A]): TerminationDirective = {
    ctx.absorbTermination()
  }
}

object SamplingWindow {
  def apply[A](size: Int)(action: List[A] => Unit): SamplingWindow[A] = {
    new SamplingWindow(size, action)
  }
}
