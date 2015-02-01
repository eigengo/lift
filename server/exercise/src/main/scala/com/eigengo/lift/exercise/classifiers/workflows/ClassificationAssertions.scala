package com.eigengo.lift.exercise.classifiers.workflows

object ClassificationAssertions {

  /**
   * Facts that may hold of sensor data
   */
  sealed trait Fact
  case class Gesture(name: String, matchProbability: Double) extends Fact
  case object Unknown extends Fact

  /**
   * Quantifier-free assertions that may hold of sensor data
   */
  sealed trait Assertion
  case class Predicate(fact: Fact) extends Assertion
  case object True extends Assertion
  case object False extends Assertion
  case class Conjunction(assert1: Assertion, assert2: Assertion, assertRemaining: Assertion*) extends Assertion
  case class Disjunction(assert1: Assertion, assert2: Assertion, assertRemaining: Assertion*) extends Assertion

  /**
   * Bind an assertion to a sensor data value. In doing this, assertion is true for that value.
   *
   * @param assertion assertion that is true for the sensor data value
   * @param value     sensor data that assertion holds for
   */
  case class Bind[A](assertion: Assertion, value: A)

}
