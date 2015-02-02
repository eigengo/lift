package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.stage.{TerminationDirective, Directive, Context, PushPullStage}
import scala.collection.mutable

/**
 * Streaming stage that buffers events and slides a window over streaming input data. Transmits each observed window
 * downstream.
 *
 * @param size size of the internal buffer and so the sliding window size
 */
class SlidingWindow[A] private (size: Int) extends PushPullStage[A, List[A]] {
  require(size > 0)

  private val buffer = mutable.Queue[A]()
  private var isSaturated = false

  override def onPush(elem: A, ctx: Context[List[A]]): Directive = {
    if (buffer.length == size) {
      // Buffer is full, so push new window
      buffer.dequeue()
      buffer.enqueue(elem)
      ctx.push(buffer.toList)
    } else {
      // Buffer is not yet full, so keep consuming from our upstream
      buffer.enqueue(elem)
      if (buffer.length == size) {
        // Buffer has become full, so push new window and record saturation
        isSaturated = true
        ctx.push(buffer.toList)
      } else {
        ctx.pull()
      }
    }
  }

  override def onPull(ctx: Context[List[A]]): Directive = {
    if (ctx.isFinishing) {
      // Streaming stage is shutting down, so we ensure that all buffer elements are flushed prior to finishing
      if (buffer.isEmpty) {
        // Buffer is empty, so we simply finish
        ctx.finish()
      } else if (buffer.length == 1) {
        // Buffer is non-empty, so empty it by sending undersized (non-empty) truncated window sequence and finish
        if (isSaturated) {
          // Buffer was previously saturated, so head element has already been seen
          buffer.dequeue()
          ctx.finish()
        } else {
          // Buffer was never saturated, so head element needs to be pushed
          ctx.pushAndFinish(List(buffer.dequeue()))
        }
      } else {
        // Buffer is non-empty, so empty it by sending undersized (non-empty) truncated window sequence - we will eventually finish here
        if (isSaturated) {
          // Buffer was previously saturated, so head element has already been seen
          buffer.dequeue()
          ctx.push(buffer.toList)
        } else {
          // Buffer was never saturated, so head element should be part of truncated window
          val window = buffer.toList
          buffer.dequeue()
          ctx.push(window)
        }
      }
    } else {
      ctx.pull()
    }
  }

  override def onUpstreamFinish(ctx: Context[List[A]]): TerminationDirective = {
    ctx.absorbTermination()
  }
}

object SlidingWindow {
  def apply[A](size: Int): SlidingWindow[A] = {
    new SlidingWindow(size)
  }
}
