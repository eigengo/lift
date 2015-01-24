package com.eigengo.lift.exercise

import akka.actor.{Props, Actor}
import com.eigengo.lift.exercise.UserExercisesClassifier._
import scala.util.Random
import UserExercises._

/**
 * Companion object for the classifier
 */
object UserExercisesClassifier {
  val props: Props = Props[UserExercisesClassifier]

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
  case class ClassificationExamples(sessionProps: SessionProps)
  
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
 * Match the received exercise data using the given model
 */
class UserExercisesClassifier extends Actor {
  val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )
  val metadata = ModelMetadata(2)

  private def randomExercise(sessionProps: SessionProps): ClassifiedExercise = {
    val mgk = Random.shuffle(sessionProps.muscleGroupKeys).head
    exercises.get(mgk).fold[ClassifiedExercise](UnclassifiedExercise(metadata))(es ⇒ FullyClassifiedExercise(metadata, 1.0, Exercise(Random.shuffle(es).head, None, None)))
  }

  override def receive: Receive = {
    case ClassifyExerciseEvt(sessionProps, sdwls) =>
      sdwls.foreach { sdwl ⇒ sdwl.data}
        sdwls.foreach { sdwl ⇒

          sdwl.data.foreach {
            case AccelerometerData(sr, values) ⇒
              val xs = values.map(_.x)
              val ys = values.map(_.y)
              val zs = values.map(_.z)
              println(s"****** X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
          }
        }
      sender() ! randomExercise(sessionProps)
    case ClassificationExamples(sessionProps) ⇒
      sender() ! List(Exercise("chest press", Some(1.0), Some(Metric(80.0, Mass.Kilogram))), Exercise("foobar", Some(1.0), Some(Metric(50.0, Distance.Kilometre))), Exercise("barfoo", Some(1.0), Some(Metric(10.0, Distance.Kilometre))))
  }

}
