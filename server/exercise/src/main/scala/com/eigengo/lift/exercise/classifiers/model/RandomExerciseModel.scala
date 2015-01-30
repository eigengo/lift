package com.eigengo.lift.exercise.classifiers.model

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.UserExercises.{ClassifyExerciseEvt, ModelMetadata}
import com.eigengo.lift.exercise.UserExercisesClassifier.{FullyClassifiedExercise, UnclassifiedExercise, ClassifiedExercise}
import scala.util.Random
import scalaz.\/-

/**
 * Random exercising model. Updates are simply printed out and queries always succeed (by sending a random message to
 * the listening actor).
 */
object RandomExerciseModel extends ExerciseModel {

  import ExerciseModel._

  val name = "random"

  type Result = ClassifiedExercise

  private val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )
  private val metadata = ModelMetadata(2)

  private def randomExercise(sessionProps: SessionProperties): ClassifiedExercise = {
    val mgk = Random.shuffle(sessionProps.muscleGroupKeys).head
    exercises.get(mgk).fold[ClassifiedExercise](UnclassifiedExercise(metadata))(es ⇒ FullyClassifiedExercise(metadata, 1.0, Exercise(Random.shuffle(es).head, None, None)))
  }

  // No update occurs here, we simply print out a summary of the received data
  def update[A <: SensorData](sdwls: ClassifyExerciseEvt[A]) = {
    sdwls.sensorData.foreach { sdwl =>
      sdwl.data.foreach {
        case AccelerometerData(sr, values) =>
          val xs = values.map(_.x)
          val ys = values.map(_.y)
          val zs = values.map(_.z)
          println(s"****** X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
      }
    }
  }

  // All queries evaluate to a random value
  def evaluate(query: Query) = {
    \/-(randomExercise(query.session))
  }

}
