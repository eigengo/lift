package com.eigengo.lift.exercise.classifiers.workflows

import com.eigengo.lift.exercise._

object ClassificationAssertions {

  /**
   * Facts that may hold of sensor data. Facts are presented in positive/negative pairs. This allows us to keep
   * assertions in negation normal form (NNF).
   */
  trait Fact
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
   * Bind inferred (e.g. machine learnt) assertions to sensors in a network of sensorse.
   *
   * @param wrist   facts true of this location
   * @param waist   facts true of this location
   * @param foot    facts true of this location
   * @param chest   facts true of this location
   * @param unknown facts true of this location
   * @param value   raw sensor network data that assertion holds for
   */
  case class BindToSensors(wrist: Set[Fact], waist: Set[Fact], foot: Set[Fact], chest: Set[Fact], unknown: Set[Fact], value: SensorNetValue) {
    val toMap = Map[SensorDataSourceLocation, Set[Fact]](
      SensorDataSourceLocationWrist -> wrist,
      SensorDataSourceLocationWaist -> waist,
      SensorDataSourceLocationFoot -> foot,
      SensorDataSourceLocationChest -> chest,
      SensorDataSourceLocationAny -> unknown
    )
  }

  object BindToSensors {
    def apply(sensorMap: Map[SensorDataSourceLocation, Set[Fact]], value: SensorNetValue) =
      new BindToSensors(
        sensorMap(SensorDataSourceLocationWrist),
        sensorMap(SensorDataSourceLocationWaist),
        sensorMap(SensorDataSourceLocationFoot),
        sensorMap(SensorDataSourceLocationChest),
        sensorMap(SensorDataSourceLocationAny),
        value
      )
  }

}
