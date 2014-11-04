package com.eigengo.pe.exercise

import akka.actor.Props
import akka.persistence.{PersistentView, SnapshotOffer}
import com.eigengo.pe.actors
import com.eigengo.pe.push.UserPushNotification

/**
 * View that handles processing the events, delegates to the classifiers,
 * and provides the query functions.
 */
class UserExerciseView extends PersistentView {
  import UserExerciseProtocol._
  import UserPushNotification._
  import actors._

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
    case e@ExerciseDataEvt(data) if isPersistent =>
      context.actorSelection("*") ! e

    // classification received
    case e@ClassifiedExercise(confidence, exercise) =>
      if (confidence > 0.0) exercises = e :: exercises
      saveSnapshot(exercises)
      // notice the lookup rather than injection of the /user/push actor
      exercise.foreach(e => pushNotification.apply ! DefaultMessage(e, Some(1), Some("default")))

    // query for exercises
    case GetExercises =>
      sender() ! exercises
  }
}
