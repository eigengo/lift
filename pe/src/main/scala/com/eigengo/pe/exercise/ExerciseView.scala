package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentView, SnapshotOffer}
import com.eigengo.pe.push.UserPushNotification
import com.eigengo.pe.{AccelerometerData, actors}

object ExerciseView {
  val shardName: String = "exercise-view-shard"
  val props: Props = Props[ExerciseView]

  def lookup(implicit arf: ActorRefFactory): ActorRef = actors.shard.lookup(arf, shardName)

  case class GetExercises(userId: UUID)

  /**
   * The event with processed fitness data into ``List[AccelerometerData]``
   * @param data the accelerometer data
   */
  case class ExerciseDataEvt(userId: UUID, data: List[AccelerometerData])

  val idExtractor: ShardRegion.IdExtractor = {
    case msg@GetExercises(userId) ⇒ (userId.toString, msg)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case GetExercises(userId) ⇒ (math.abs(userId.hashCode()) % 100).toString
  }

}

/**
 * View that handles processing the events, delegates to the classifiers,
 * and provides the query functions.
 */
class ExerciseView extends PersistentView with ActorLogging {
  import com.eigengo.pe.exercise.ExerciseClassifier._
  import com.eigengo.pe.exercise.ExerciseView._
  import com.eigengo.pe.push.UserPushNotification._

  type Exercises = Map[UUID, List[ClassifiedExercise]]
  private var exercises: Exercises = Map.empty

  context.actorOf(Props(classOf[ExerciseClassifier], NaiveModel))
  context.actorOf(Props(classOf[ExerciseClassifier], WaveletModel))
  context.actorOf(Props(classOf[ExerciseClassifier], DynamicTimeWrappingModel))

  override val viewId: String = "user-exercise-view"

  override val persistenceId: String = "user-exercise-persistence"

  override def receive: Receive = {
    // Remember to handle snapshot offers when using ``saveSnapshot``
    case SnapshotOffer(metadata, offeredSnapshot: Exercises) =>
      exercises = offeredSnapshot

    // send the exercise to be classified to the children
    case e@ExerciseDataEvt(userId, data) if isPersistent ⇒
      context.actorSelection("*") ! e

    // classification received
    case (userId: UUID, e@ClassifiedExercise(confidence, exercise)) =>
      log.debug(s"ClassifiedExercise $e")
      if (confidence > 0.0) {
        val userExercises = e :: exercises.getOrElse(userId, List.empty[ClassifiedExercise])
        exercises = exercises + (userId → userExercises)
        exercise.foreach(e => UserPushNotification.lookup ! DefaultMessage(e, Some(1), Some("default")))
      }
      saveSnapshot(exercises)

    // query for exercises
    case GetExercises(userId) =>
      sender() ! exercises.getOrElse(userId, List.empty[ClassifiedExercise])
  }
}
