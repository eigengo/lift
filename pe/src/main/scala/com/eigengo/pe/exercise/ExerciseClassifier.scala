package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.Actor
import com.eigengo.pe.AccelerometerData

import scala.util.Random
import ExerciseClassifier._

/**
 * The exercise classification model
 */
sealed trait ExerciseModel {
  def apply(data: AccelerometerData): ClassificationResult
}

/**
 * Implementation left as an exercise
 */
case object WaveletModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassificationResult = ClassificationResult(0.0, None)
}

/**
 * Implementation left as an exercise
 */
case object DynamicTimeWrappingModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassificationResult = ClassificationResult(0.0, None)
}

/**
 * This is the only implementation I can have a go at!
 */
case object NaiveModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassificationResult = ClassificationResult(1.0, Some("Goku was your spotter"))
}

object ExerciseClassifier {
  /** The exercise */
  type Exercise = String
  
  case class ClassificationResult(confidence: Double, exercise: Option[Exercise])

  /**
   * Classified exercise
   *
   * @param userId the user identity
   * @param result the classification result
   */
  case class ClassifiedExercise(userId: UUID, result: ClassificationResult)

  /**
   * Classify exercise request
   * 
   * @param userId the user identity
   * @param data the accelerometer data
   */
  case class ClassifyExercise(userId: UUID, data: List[AccelerometerData])
}

/**
 * Match the received exercise data using the given model
 * @param model the model
 */
class ExerciseClassifier(model: ExerciseModel) extends Actor {

  override def receive: Receive = {
    case ClassifyExercise(userId, data) if data.nonEmpty =>
      Thread.sleep(300 + Random.nextInt(1000)) // Is complicated, no? :)

      val ad = data.foldRight(data.last)((res, ad) => ad.copy(values = ad.values ++ res.values))
      sender() ! ClassifiedExercise(userId, model(ad))
  }

}
