package com.eigengo.lift.exercise.classifiers.model

import akka.actor.{ActorLogging, Actor}
import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.UserExercises.ModelMetadata
import com.eigengo.lift.exercise.UserExercisesClassifier.{FullyClassifiedExercise, UnclassifiedExercise, ClassifiedExercise}
import com.eigengo.lift.exercise.classifiers.ExerciseModel.Query
import scala.util.Random
import scalaz.{\/, -\/, \/-}

/**
 * Random exercising model. Updates are simply printed out and queries always succeed (by sending a random message to
 * the listening actor).
 */
class RandomExerciseModel(val sessionProps: SessionProperties, val watch: Set[Query])
  extends ExerciseModel[ClassifiedExercise]
  with Actor
  with ActorLogging {

  import ExerciseModel._

  val name = "random"

  private val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )
  private val metadata = ModelMetadata(2)

  private def randomExercise(): ClassifiedExercise = {
    val mgk = Random.shuffle(sessionProps.muscleGroupKeys).head
    exercises.get(mgk).fold[ClassifiedExercise](UnclassifiedExercise(metadata))(es ⇒ FullyClassifiedExercise(metadata, 1.0, Exercise(Random.shuffle(es).head, None, None)))
  }

  override def receive = {
    // No update actually occurs for this model, we simply print out a summary of the received data and return model checking results
    case Update(sdwls) =>
      sdwls.sensorData.foreach { sdwl =>
        sdwl.data.foreach {
          case AccelerometerData(sr, values) =>
            val xs = values.map(_.x)
            val ys = values.map(_.y)
            val zs = values.map(_.z)
            println(s"****** X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
        }
      }
      watch.foreach { query =>
        evaluate(query) match {
          case \/-(result) =>
            sender() ! result

          case -\/(None) =>
            // we ignore this case as query evaluated to be false without any error

          case -\/(Some(err)) =>
            log.error(err)
        }
      }
  }

  // All queries evaluate to a random value
  def evaluate(query: Query): Option[String] \/ ClassifiedExercise = {
    \/-(randomExercise())
  }

}

object RandomExerciseModel {
  def apply(sessionProps: SessionProperties, watch: Set[Query]) = new RandomExerciseModel(sessionProps, watch)
}
