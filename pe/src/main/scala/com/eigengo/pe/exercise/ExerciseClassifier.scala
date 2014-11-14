package com.eigengo.pe.exercise

import akka.actor.Actor
import com.eigengo.pe.AccelerometerData
import com.eigengo.pe.exercise.ExerciseClassifier._

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
  override def apply(data: AccelerometerData): ClassifiedExercise = ClassifiedExercise(0.0, None, None)
}

/**
 * Implementation left as an exercise
 */
case object DynamicTimeWrappingModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassifiedExercise = ClassifiedExercise(0.0, None, None)
}

/**
 * This is the only implementation I can have a go at!
 */
case object NaiveModel extends ExerciseModel {
  override def apply(data: AccelerometerData): ClassifiedExercise = ClassifiedExercise(1.0, Some("Goku was your spotter!"), Some(1))
}

object ExerciseClassifier {

  case class ClassifiedExercise(confidence: Double, name: Option[ExerciseName], intensity: Option[ExerciseIntensity])

}

/**
 * Match the received exercise data using the given model
 * @param model the model
 */
class ExerciseClassifier(model: ExerciseModel) extends Actor {

  override def receive: Receive = {
    case ad@AccelerometerData(samplingRate, values) =>
      Thread.sleep(300 + Random.nextInt(1000)) // Is complicated, no? :)

      sender() ! model(ad)
  }

}
