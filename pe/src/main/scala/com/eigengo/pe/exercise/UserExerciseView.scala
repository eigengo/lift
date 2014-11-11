package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, ActorLogging, Props}
import akka.contrib.pattern.{ClusterSharding, ShardRegion}
import akka.persistence.{SnapshotOffer, PersistentView}
import com.eigengo.pe.exercise.UserExercise.UserExerciseDataEvt
import com.eigengo.pe.push.UserPushNotification

object UserExerciseView {
  val shardName: String = "user-exercise-view"
  val props: Props = Props[UserExerciseView]

  def lookup(implicit system: ActorSystem): ActorRef = ClusterSharding(system).shardRegion(shardName)

  sealed trait Query {
    def userId: UUID
  }
  case class GetExercises(userId: UUID) extends Query

  val idExtractor: ShardRegion.IdExtractor = {
    case UserExerciseDataEvt()
    case GetExercises(userId) ⇒ (userId.toString, UserGetExercises)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case query: Query => (math.abs(query.userId.hashCode()) % 100).toString
  }


  /**
   * List all user's exercises
   */
  case object UserGetExercises

}

/**
 * View that handles processing the events, delegates to the classifiers,
 * and provides the query functions.
 */
class UserExerciseView extends PersistentView with ActorLogging {
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
      exercise.foreach(e => UserPushNotification.lookup ! DefaultMessage(e, Some(1), Some("default")))

    // query for exercises
    case UserGetExercises =>
      sender() ! exercises
  }
}
