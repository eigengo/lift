package com.eigengo.lift.exercise.classifiers.workflows

object ClassificationAssertions {

  /**
   * Facts that may hold of sensor data. Facts are presented in positive/negative pairs. This allows us to keep
   * assertions in negation normal form (NNF).
   */
  sealed trait Fact
  case object True extends Fact
  case object False extends Fact
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
    case True =>
      False

    case False =>
      True

    case Gesture(name, probability) =>
      NegGesture(name, probability)

    case NegGesture(name, probability) =>
      Gesture(name, probability)
  }

  /**
   * Bind an assertion to a sensor data value. In doing this, assertion is true for that value.
   *
   * @param assertion assertion that is true for the sensor data value
   * @param value     sensor data that assertion holds for
   */
  case class Bind[A](assertion: Set[Fact], value: A) {
    // `assertion` must be consistent!
    require(
      assertion.forall(f => !assertion.contains(not(f)))
    )
    require(
      assertion.forall {
        case Gesture(nm, prob1) =>
          assertion.forall { case NegGesture(`nm`, prob2) => prob1 <= prob2; case _ => true }

        case _ =>
          true
      }
    )
  }

}
