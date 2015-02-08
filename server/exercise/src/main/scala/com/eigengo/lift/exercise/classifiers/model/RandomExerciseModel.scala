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
class RandomExerciseModel[A <: SensorData](val sessionProps: SessionProperties, val negativeWatch: Set[Query] = Set.empty, val positiveWatch: Set[Query] = Set.empty)
  extends ExerciseModel[A]
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
    }
  }

  val workflow =
    Flow[Map[SensorDataSourceLocation, A]]
      .map { sdwls =>
        val classification = randomExercise()
        sdwls.mapValues(sd => Bind(classification, sd))
      }

  def evaluate(query: Query)(current: Map[SensorDataSourceLocation, Bind[A]], lastState: Boolean) =
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
