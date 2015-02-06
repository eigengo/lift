package com.eigengo.lift.exercise.classifiers.workflows

object ClassificationAssertions {

  /**
   * Facts that may hold of sensor data. Facts are presented in positive/negative pairs. This allows us to keep
   * assertions in negation normal form (NNF).
   */
  sealed trait Fact
  /**
   * Named gesture matches with probability >= `matchProbability`
   */
  case class Gesture(name: String, matchProbability: Double) extends Fact
  /**
   * Named gesture matches with probability < `matchProbability`
   */
  case class NegGesture(name: String, matchProbability: Double) extends Fact

  /**
   * Convenience function that provides negation on facts, whilst keeping them in NNF. Translation is linear in the
   * size of the fact.
   */
  def not(fact: Fact): Fact = fact match {
    case Gesture(name, probability) =>
      NegGesture(name, probability)

    case NegGesture(name, probability) =>
      Gesture(name, probability)
  }

  /**
   * Quantifier-free assertions that may hold of sensor data.
   */
  sealed trait Assertion
  case class Predicate(fact: Fact) extends Assertion
  case object True extends Assertion
  case object False extends Assertion
  case class Conjunction(assert1: Assertion, assert2: Assertion, remainingAsserts: Assertion*) extends Assertion
  case class Disjunction(assert1: Assertion, assert2: Assertion, remainingAsserts: Assertion*) extends Assertion

  case class Atom[Q](query: Q) extends Assertion

  /**
   * Convenience function that provides negation on assertions, whilst keeping them in NNF. Translation is linear in the
   * size of the assertion.
   */
  def not(assertion: Assertion): Assertion = assertion match {
    case Predicate(fact) =>
      Predicate(not(fact))

    case True =>
      False

    case False =>
      True

    case Conjunction(assert1, assert2, remaining @ _*) =>
      Disjunction(not(assert1), not(assert2), remaining.map(not): _*)

    case Disjunction(assert1, assert2, remaining @ _*) =>
      Conjunction(not(assert1), not(assert2), remaining.map(not): _*)
  }

  /**
   * Bind an assertion to a sensor data value. In doing this, assertion is true for that value.
   *
   * @param assertion assertion that is true for the sensor data value
   * @param value     sensor data that assertion holds for
   */
  case class Bind[A](assertion: Option[Assertion], value: A)

}
