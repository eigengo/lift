package org.eigengo.pe.exercise

import akka.actor.Actor
import org.eigengo.pe.AccelerometerData
import org.eigengo.pe.exercise.UserExerciseProtocol.ClassifiedExercise

import scala.util.Random

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

/**
 * Match the received exercise data using the given model
 * @param model the model
 */
class ExerciseClassifier(model: ExerciseModel) extends Actor {
  import UserExerciseProtocol._

  override def receive: Receive = {
    case ExerciseDataEvt(data) if data.nonEmpty =>
      Thread.sleep(300 + Random.nextInt(1000)) // Is complicated, no? :)

      val ad = data.foldRight(data.last)((res, ad) => ad.copy(values = ad.values ++ res.values))
      sender() ! model(ad)
  }

}
