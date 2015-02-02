package com.eigengo.lift.exercise

import akka.actor.{Props, Actor}
import com.eigengo.lift.exercise.i8tn.Localized
import com.eigengo.lift.exercise.UserExercisesClassifier._
import com.eigengo.lift.exercise.classifiers.model.RandomExerciseModel
import com.eigengo.lift.exercise.classifiers.ExerciseModel
import UserExercises._

/**
 * Companion object for the classifier
 */
object UserExercisesClassifier {
  // By default, we configure exercise classification iwth a random model
  val props: Props = Props(new UserExercisesClassifier(RandomExerciseModel))

  /**
   * Muscle group information
   *
   * @param key the key
   * @param title the title
   * @param exercises the suggested exercises
   */
  case class MuscleGroup(key: String, title: String, exercises: List[String])

  val supportedMuscleGroups = List(
    MuscleGroup(key = "legs",  title = Localized(_.exerciseLegs),  exercises = List(
      Localized(_.exerciseLegsSquat),
      Localized(_.exerciseLegsLegPress),
      Localized(_.exerciseLegsLegExtension),
      Localized(_.exerciseLegsLegCurl),
      Localized(_.exerciseLegsLunge))),
    MuscleGroup(key = "core",  title = Localized(_.exerciseCore),  exercises = List(
      Localized(_.exerciseCoreCrunch),
      Localized(_.exerciseCoreSideBend),
      Localized(_.exerciseCoreCableCrunch),
      Localized(_.exerciseCoreSitup),
      Localized(_.exerciseCoreLegRaises))),
    MuscleGroup(key = "back",  title = Localized(_.exerciseBack),  exercises = List(
      Localized(_.exerciseBackPullup),
      Localized(_.exeriseBackRow),
      Localized(_.exerciseBackDeadlift),
      Localized(_.exerciseBackHyperExtension))),
    MuscleGroup(key = "arms",  title = Localized(_.exerciseArms),  exercises = List(
      Localized(_.exerciseArmsBicepCurl),
      Localized(_.exerciseArmsHammerCurl),
      Localized(_.exerciseArmsPronatedCurl),
      Localized(_.exericseArmsTricepPushdown),
      Localized(_.exerciseArmsTricepOverheadExtension),
      Localized(_.exerciseArmsDip),
      Localized(_.exerciseArmsCloseGripBenchPress))),
    MuscleGroup(key = "chest", title = Localized(_.exerciseChest), exercises = List(
      Localized(_.exerciseChest),
      Localized(_.exerciseChestButterfly),
      Localized(_.exerciseChestCableCrossover),
      Localized(_.exerciseChestInclinePress),
      Localized(_.exerciseChestPushup))),
    MuscleGroup(key = "shoulders", title = Localized(_.exerciseShoulders), exercises = List(
      Localized(_.exerciseShouldersShoulderPress),
      Localized(_.exerciseShouldersLateralRaise),
      Localized(_.exerciseShouldersFrontRaise),
      Localized(_.exerciseShouldersRearRaise),
      Localized(_.exerciseShouldersUprightRow),
      Localized(_.exerciseShouldersShrug))),
    MuscleGroup(key = "cardiovascular", title = Localized(_.exerciseCardio), exercises = List(
      Localized(_.exerciseCardioRunning),
      Localized(_.exerciseCardioCycling),
      Localized(_.exerciseCardioSwimming),
      Localized(_.exerciseCardioElliptical),
      Localized(_.exerciseCardioRowing)))
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
class UserExercisesClassifier(model: ExerciseModel) extends Actor {

  import ExerciseModel._

  override def receive: Receive = {
    case event @ ClassifyExerciseEvt(sessionProps, _) =>
      model.update(event)
      model.query(True(sessionProps), sender())

    case ClassificationExamples(sessionProps) ⇒
      sender() ! List(Exercise(Localized(_.exerciseChestChestPress), Some(1.0), Some(Metric(80.0, Mass.Kilogram))), Exercise(Localized(_.exerciseArmsBicepCurl), Some(1.0), Some(Metric(50.0, Distance.Kilometre))), Exercise("barfoo", Some(1.0), Some(Metric(10.0, Distance.Kilometre))))
  }

}
