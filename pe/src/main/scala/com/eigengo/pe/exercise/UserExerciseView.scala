package com.eigengo.pe.exercise

import akka.actor.{ActorLogging, Props}
import akka.persistence.{SnapshotOffer, PersistentView}

object UserExerciseView {
  val name: String = "user-exercise-view"

  val props: Props = Props[UserExerciseView]


  /**
   * List all user's exercises
   */
  case object GetExercises

}

/**
 * View that handles processing the events, delegates to the classifiers,
 * and provides the query functions.
 */
class UserExerciseView extends PersistentView with ActorLogging {
  import com.eigengo.pe.actors._
  import com.eigengo.pe.exercise.UserExercise._
  import com.eigengo.pe.exercise.UserExerciseView._
  import com.eigengo.pe.push.UserPushNotification._
  import ExerciseClassifier._

  private var exercises: List[ClassifiedExercise] = Nil

  context.actorOf(Props(classOf[ExerciseClassifier], NaiveModel))
  context.actorOf(Props(classOf[ExerciseClassifier], WaveletModel))
  context.actorOf(Props(classOf[ExerciseClassifier], DynamicTimeWrappingModel))

  override val viewId: String = "user-exercise-view"

  override val persistenceId: String = "user-exercise-persistence"

  override def receive: Receive = {
    // Remember to handle snapshot offers when using ``saveSnapshot``
    case SnapshotOffer(metadata, offeredSnapshot: List[ClassifiedExercise @unchecked]) =>
      exercises = offeredSnapshot

    // send the exercise to be classified to the children
    case e@UserExerciseDataEvt(data) if isPersistent =>
      context.actorSelection("*") ! e

    // classification received
    case e@ClassifiedExercise(confidence, exercise) =>
      log.debug(s"ClassifiedExercise $e")
      if (confidence > 0.0) exercises = e :: exercises
      saveSnapshot(exercises)
      // notice the lookup rather than injection of the /user/push actor
      exercise.foreach(e => pushNotification.apply ! DefaultMessage(e, Some(1), Some("default")))

    // query for exercises
    case GetExercises =>
      sender() ! exercises
  }
}
