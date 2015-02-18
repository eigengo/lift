package com.eigengo.lift.exercise

import akka.actor.{Props, Actor}
import com.eigengo.lift.exercise.UserExercisesClassifier._
import UserExercises._

/**
 * Companion object for the classifier
 */
object UserExercisesClassifier {
  def props(sessionProps: SessionProperties, modelProps: Props): Props =
    Props(new UserExercisesClassifier(sessionProps, modelProps))

  /**
   * Muscle group information
   *
   * @param key the key
   * @param title the title
   * @param exercises the suggested exercises
   */
  case class MuscleGroup(key: String, title: String, exercises: List[String])

  val supportedMuscleGroups = List(
    MuscleGroup(key = "legs",  title = "Legs",  exercises = List("squat", "leg press", "leg extension", "leg curl", "lunge")),
    MuscleGroup(key = "core",  title = "Core",  exercises = List("crunch", "side bend", "cable crunch", "sit up", "leg raises")),
    MuscleGroup(key = "back",  title = "Back",  exercises = List("pull up", "row", "deadlift", "hyper-extension")),
    MuscleGroup(key = "arms",  title = "Arms",  exercises = List("bicep curl", "hammer curl", "pronated curl", "tricep push down", "tricep overhead extension", "tricep dip", "close-grip bench press")),
    MuscleGroup(key = "chest", title = "Chest", exercises = List("chest press", "butterfly", "cable cross-over", "incline chest press", "push up")),
    MuscleGroup(key = "shoulders", title = "Shoulders", exercises = List("shoulder press", "lateral raise", "front raise", "rear raise", "upright row", "shrug")),
    MuscleGroup(key = "cardiovascular", title = "Cardiovascular", exercises = List("running", "cycling", "swimming", "elliptical", "rowing"))
  )

  /**
   * Provides List[Exercise] as examples of exercises for the given ``sessionProps``
   * @param sessionProps the session props
   */
  case class ClassificationExamples(sessionProps: SessionProperties)
  
  /**
   * ADT holding the classification result
   */
  sealed trait ClassifiedExercise

  /**
   * Known exercise with the given confidence, name and optional intensity
   * @param metadata the model metadata
   * @param confidence the confidence
   * @param exercise the exercise
   */
  case class FullyClassifiedExercise(metadata: ModelMetadata, confidence: Double, exercise: Exercise) extends ClassifiedExercise

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
 * Match the received exercise data using the given model.
 */
class UserExercisesClassifier(sessionProperties: SessionProperties, modelProps: Props) extends Actor {

  // Issue "callback" (via sender actor reference) whenever we detect a tap gesture with a matching probability >= 0.80
  val model = context.actorOf(modelProps)

  override def receive: Receive = {
    // TODO: refactor code so that the following assumptions may be weakened further!
    case sdwls: ClassifyExerciseEvt =>
      require(
        sdwls.sensorData.map(_.location).toSet == Sensor.sourceLocations && sdwls.sensorData.forall(_.data.nonEmpty),
        "all sensor locations are present in the `ClassifyExerciseEvt` instance and have data"
      )
      // (SensorDataSourceLocation, Int) -> List[SensorData]
      val sensorMap: Map[SensorDataSourceLocation, List[List[SensorData]]] = sdwls.sensorData.groupBy(_.location).mapValues(_.map(_.data))
      val blockSize = sensorMap(SensorDataSourceLocationWrist).head.length
      require(
        sensorMap.values.forall(_.forall(_.length == blockSize)),
        "all sensor data location points have a common data length"
      )

      (0 until blockSize).foreach { block =>
        val sensorEvent = sensorMap.map { case (loc, data) => (loc, (0 until data.size).map(point => sensorMap(loc)(point)(block)).toVector) }.toMap

        model.tell(SensorNet(sensorEvent), sender())
      }

    case ClassificationExamples(_) =>
      val examples = sessionProperties.muscleGroupKeys.foldLeft(List.empty[Exercise]) { (r, b) ⇒
        supportedMuscleGroups
          .find(_.key == b)
          .map { mg ⇒ r ++ mg.exercises.map(exercise ⇒ Exercise(exercise, None, None)) }
          .getOrElse(r)
      }

      sender() ! examples
  }

}
