package com.eigengo.lift.exercise

import akka.actor.Actor
import com.eigengo.lift.common.UserId
import com.eigengo.lift.exercise.ExerciseClassifier._

import scala.util.Random

/**
 * The exercise classification model
 */
sealed trait ExerciseModel {
  def apply(classify: Classify): ClassifiedExercise
}

/**
 * Implementation left as an exercise
 */
case object WaveletModel extends ExerciseModel {
  val metadata = ModelMetadata(1)
  override def apply(classify: Classify): ClassifiedExercise = UnclassifiedExercise(metadata)
}

/**
 * Implementation left as an exercise
 */
case object DynamicTimeWrappingModel extends ExerciseModel {
  val metadata = ModelMetadata(1)
  override def apply(classify: Classify): ClassifiedExercise = UnclassifiedExercise(metadata)
}

/**
 * This is the only implementation I can have a go at!
 */
case object NaiveModel extends ExerciseModel {
  val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )
  val metadata = ModelMetadata(2)

  private def randomExercise(sessionProps: SessionProps): ClassifiedExercise = {
    val mgk = Random.shuffle(sessionProps.muscleGroupKeys).head
    exercises.get(mgk).fold[ClassifiedExercise](UnclassifiedExercise(metadata))(es ⇒ FullyClassifiedExercise(metadata, 1.0, Random.shuffle(es).head, None))
  }

  override def apply(classify: Classify): ClassifiedExercise = {
    val xs = classify.ad.values.map(_.x)
    val ys = classify.ad.values.map(_.y)
    val zs = classify.ad.values.map(_.z)
    println(s"****** X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
    randomExercise(classify.sessionProps)
  }
}

/**
 * Companion object for the classifier
 */
object ExerciseClassifier {

  /**
   * Classify the given accelerometer data together with session information
   * @param sessionProps the session
   * @param ad the accelerometer data
   */
  case class Classify(sessionProps: SessionProps, ad: AccelerometerData)

  /**
   * Model version and other metadata
   * @param version the model version
   */
  case class ModelMetadata(version: Int)

  /**
   * ADT holding the classification result
   */
  sealed trait ClassifiedExercise

  /**
   * Known exercise with the given confidence, name and optional intensity
   * @param metadata the model metadata
   * @param confidence the confidence
   * @param name the exercise name
   * @param intensity the intensity, if known
   */
  case class FullyClassifiedExercise(metadata: ModelMetadata, confidence: Double, name: ExerciseName, intensity: Option[ExerciseIntensity]) extends ClassifiedExercise

  /**
    * Unknown exercise
   * @param metadata the model
   */
  case class UnclassifiedExercise(metadata: ModelMetadata) extends ClassifiedExercise

  /**
    * No exercise: ideally, a rest between sets, or just plain old not working out
   * @param metadata the model
   */
  case class NoExercise(metadata: ModelMetadata) extends ClassifiedExercise

  /**
   * The user has tapped the input device
   */
  case object Tap extends ClassifiedExercise
}

/**
 * Match the received exercise data using the given model
 * @param model the model
 */
class ExerciseClassifier(model: ExerciseModel) extends Actor {

  override def receive: Receive = {
    case c@Classify(_, _) =>
      Thread.sleep(100 + Random.nextInt(100)) // Is complicated, no? :)

      sender() ! model(c)
  }

}
