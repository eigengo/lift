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
class UserExercisesClassifier(sessionProps: SessionProperties, modelProps: Props) extends Actor {

  // Issue "callback" (via sender actor reference) whenever we detect a tap gesture with a matching probability >= 0.80
  val model = context.actorOf(modelProps)

  override def receive: Receive = {
    // TODO: refactor code so that the following assumptions may be weakened further!
    case sdwls: ClassifyExerciseEvt =>
      require(
        sdwls.sensorData.map(_.location).toSet == Sensor.sourceLocations && sdwls.sensorData.map(_.location).size == Sensor.sourceLocations.size,
        "for each sensor location, there is a unique and corresponding member in the sensor data for the `ClassifyExerciseEvt` instance"
      )
      val sensorMap = sdwls.sensorData.groupBy(_.location).mapValues(_.flatMap(_.data))
      val blockSize = sensorMap(SensorDataSourceLocationWrist).length
      require(
        sensorMap.values.forall(_.length == blockSize),
        "all sensor data locations have a common data length"
      )

      (0 until blockSize).foreach { block =>
        val sensorEvent = sensorMap.map { case (loc, _) => (loc, sensorMap(loc)(block)) }.toMap

        model.tell(SensorNet(sensorEvent), sender())
      }

    case ClassificationExamples(_) =>
      sender() ! List(Exercise("chest press", Some(1.0), Some(Metric(80.0, Mass.Kilogram))), Exercise("foobar", Some(1.0), Some(Metric(50.0, Distance.Kilometre))), Exercise("barfoo", Some(1.0), Some(Metric(10.0, Distance.Kilometre))))
  }

}
