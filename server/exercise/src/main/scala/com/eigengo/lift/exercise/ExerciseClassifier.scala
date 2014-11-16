package com.eigengo.lift.exercise

import akka.actor.Actor
import com.eigengo.lift.exercise.ExerciseClassifier.{Classify, FullyClassifiedExercise, UnclassifiedExercise, ClassifiedExercise}

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
  override def apply(classify: Classify): ClassifiedExercise = UnclassifiedExercise(classify.session)
}

/**
 * Implementation left as an exercise
 */
case object DynamicTimeWrappingModel extends ExerciseModel {
  override def apply(classify: Classify): ClassifiedExercise = UnclassifiedExercise(classify.session)
}

/**
 * This is the only implementation I can have a go at!
 */
case object NaiveModel extends ExerciseModel {
  override def apply(classify: Classify): ClassifiedExercise =
    FullyClassifiedExercise(classify.session, 1.0, "Goku was your spotter", Some(Random.nextDouble()))
}

/**
 * Companion object for the classifier
 */
object ExerciseClassifier {

  case class Classify(session: Session, ad: AccelerometerData)

  /**
   * ADT holding the classification result
   */
  sealed trait ClassifiedExercise

  /**
   * Known exercise with the given confidence, name and optional intensity
   * @param session the session
   * @param confidence the confidence
   * @param name the exercise name
   * @param intensity the intensity, if known
   */
  case class FullyClassifiedExercise(session: Session, confidence: Double, name: ExerciseName, intensity: Option[ExerciseIntensity]) extends ClassifiedExercise

  /**
   * Unknown exercise
   * @param session the session
   */
  case class UnclassifiedExercise(session: Session) extends ClassifiedExercise

}

/**
 * Match the received exercise data using the given model
 * @param model the model
 */
class ExerciseClassifier(model: ExerciseModel) extends Actor {

  override def receive: Receive = {
    case c@Classify(_, _) =>
      Thread.sleep(300 + Random.nextInt(1000)) // Is complicated, no? :)

      sender() ! model(c)
  }

}
