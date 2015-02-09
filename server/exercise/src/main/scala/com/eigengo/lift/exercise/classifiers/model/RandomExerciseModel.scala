package com.eigengo.lift.exercise.classifiers.model

import akka.actor.{ActorLogging, Actor}
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel.Query
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions
import scala.util.Random

/**
 * Random exercising model. Updates are simply printed out and queries always succeed (by sending a random message to
 * the listening actor).
 */
class RandomExerciseModel(val sessionProps: SessionProperties, val negativeWatch: Set[Query] = Set.empty, val positiveWatch: Set[Query] = Set.empty)
  extends ExerciseModel
  with Actor
  with ActorLogging {

  import ClassificationAssertions._
  import ExerciseModel._
  import RandomExerciseModel.exercises

  val name = "random"

  private def randomExercise(): Set[Fact] = {
    val mgk = Random.shuffle(sessionProps.muscleGroupKeys).head
    if (exercises.get(mgk).isEmpty) {
      Set.empty
    } else {
      val exerciseType = Random.shuffle(exercises.get(mgk).get).head

      Set(Gesture(exerciseType, 0.80))
/* TODO: need to handle multi-sensor data types!!
  // No update occurs here, we simply print out a summary of the received data
  def update[A <: SensorData](sdwls: ClassifyExerciseEvt[A]) = {
    sdwls.sensorData.foreach { sdwl =>
      sdwl.data.foreach {
        case AccelerometerData(sr, values) =>
          val xs = values.map(_.x)
          val ys = values.map(_.y)
          val zs = values.map(_.z)
          println(s"****** Acceleration ${sdwl.location} | X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
        case RotationData(_, values) ⇒
          val xs = values.map(_.x)
          val ys = values.map(_.y)
          val zs = values.map(_.z)
          println(s"****** Rotation ${sdwl.location} | X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
      }
*/
    }
  }

  val workflow = ???
  /*
    Flow[SensorNetValue]
      .map { sn =>
        val classification = randomExercise()
        Bind(classification, sn) // FIXME:
      }
*/
  def evaluate(query: Query)(current: BindToSensors, lastState: Boolean) =
    StableValue(result = true)

}

object RandomExerciseModel {
  val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )

  def apply(sessionProps: SessionProperties, negativeWatch: Set[Query], positiveWatch: Set[Query]) = new RandomExerciseModel(sessionProps, negativeWatch, positiveWatch)
}
