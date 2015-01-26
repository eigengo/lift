package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.scaladsl.FlexiMerge

/**
* Flowgraph merge node that expects a message on each of its inputs. The collection of input messages is then outputted.
* This merge node can be thought of as a generalised Zip or ZipWith node.
*
* @param size number of inputs that may be joined to this merge node
*/
class ZipSet[A] private (size: Int) extends FlexiMerge[Set[A]] {
  require(size >= 0)

  import FlexiMerge._

  val in = (0 until size).map { _ => createInputPort[A]() }.toVector

  def createMergeLogic() = new MergeLogic[Set[A]] {
    def initialState = State[ReadAllInputs](ReadAll(in)) { (ctx, _, inputs) =>
      ctx.emit(in.flatMap(port => inputs.get[A](port)).toSet)

      SameState[A]
    }

    def inputHandles(inputCount: Int) = {
      require(inputCount == size, s"ZipSet must have $size connected inputs, was $inputCount")

      in
    }

    override def initialCompletionHandling = eagerClose
  }

}

object ZipSet {
  def apply[A](size: Int) = {
    new ZipSet[A](size)
  }
}
