package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.scaladsl.FlexiMerge

/**
* Flowgraph merge node that expects a message on each of its inputs. The collection of input messages is then outputted.
* This merge node can be thought of as a generalised Zip or ZipWith node.
*
* @param size number of inputs that may be joined to this merge node
*/
abstract class ZipBundle[A] private[workflows] (size: Int) extends FlexiMerge[Set[A]] {
  require(size > 1, s"ZipBundle must have at least 2 connected inputs ($size were given)")

  import FlexiMerge._

  protected val inPorts = (0 until size).map { _ => createInputPort[A]() }.toVector

  def createMergeLogic() = new MergeLogic[Set[A]] {
    def initialState = State[ReadAllInputs](ReadAll(inPorts)) { (ctx, _, inputs) =>
      ctx.emit(inPorts.flatMap(port => inputs.get[A](port)).toSet)

      SameState[A]
    }

    def inputHandles(inputCount: Int) = {
      require(inputCount == size, s"ZipBundle must have $size connected inputs, was $inputCount")

      inPorts
    }

    override def initialCompletionHandling = eagerClose
  }

}

/**
 * Utility classes and factories for using ZipBundle. They differ in how we reference the input ports.
 *
 * ZipN   - input ports are referenced using indexes from 0 until size
 * ZipSet - input ports are referenced using a set of locations or addresses
 */

class ZipN[A] private[workflows] (size: Int) extends ZipBundle[A](size) {
  val in = inPorts
}

object ZipN {
  def apply[A](size: Int) = new ZipN[A](size)
}

class ZipSet[A, L] private[workflows] (keys: Set[L]) extends ZipBundle[A](keys.size) {
  val in = keys.zipWithIndex.map { case (key, index) => (key, inPorts(index)) }.toMap
}

object ZipSet {
  def apply[A, L](keys: Set[L]) = new ZipSet[A, L](keys)
}
