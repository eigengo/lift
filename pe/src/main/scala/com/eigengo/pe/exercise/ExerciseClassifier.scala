package com.eigengo.pe.exercise

import akka.actor.Actor
import com.eigengo.pe.AccelerometerData

import scala.util.Random
import ExerciseClassifier._

/**
 * The exercise classification model
 */
sealed trait ExerciseModel {
  def apply(data: AccelerometerData): ClassifiedExercise
}

/**
 * Implementation left as an exercise
 */
case object WaveletModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassifiedExercise = ClassifiedExercise(0.0, None)
}

/**
 * Implementation left as an exercise
 */
case object DynamicTimeWrappingModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassifiedExercise = ClassifiedExercise(0.0, None)
}

/**
 * This is the only implementation I can have a go at!
 */
case object NaiveModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassifiedExercise = ClassifiedExercise(1.0, Some("Goku was your spotter"))
}

object ExerciseClassifier {
  /** The exercise */
  type Exercise = String

  /**
   * Classified exercise
   *
   * @param confidence the classification confidence 0..1
   * @param exercise the classified exercise, if any
   */
  case class ClassifiedExercise(confidence: Double, exercise: Option[Exercise])
}

/**
 * Match the received exercise data using the given model
 * @param model the model
 */
class ExerciseClassifier(model: ExerciseModel) extends Actor {
  import UserExerciseView._

  override def receive: Receive = {
    case ExerciseDataEvt(userId, data) if data.nonEmpty =>
      Thread.sleep(300 + Random.nextInt(1000)) // Is complicated, no? :)

      val ad = data.foldRight(data.last)((res, ad) => ad.copy(values = ad.values ++ res.values))
      sender() ! (userId, model(ad))
  }

}
