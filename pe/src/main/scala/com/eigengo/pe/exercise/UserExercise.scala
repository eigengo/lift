package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorLogging, ActorRefFactory, Props, Actor}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{SnapshotOffer, PersistentActor}
import com.eigengo.pe.actors
import com.eigengo.pe.push.UserPushNotification
import com.eigengo.pe.push.UserPushNotification.DefaultMessage

object UserExercise {
  import ExerciseClassifier._
  val shardName = "user-exercise-shard"
  val props = Props[UserExercise]
  def lookup(implicit arf: ActorRefFactory) = actors.shard.lookup(arf, shardName)

  case class GetUserExercises(userId: UUID)
  case object GetExercises

  val idExtractor: ShardRegion.IdExtractor = {
    case GetUserExercises(userId) ⇒ (userId.toString, GetExercises)
    case ce@ClassifiedExercise(userId, result) ⇒ (userId.toString, ce)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case GetUserExercises(userId) ⇒ userId.toString
    case ClassifiedExercise(userId, _) ⇒ userId.toString
  }

}

class UserExercise extends PersistentActor with ActorLogging {
  import UserExercise._
  import ExerciseClassifier._
  private var exercises = List.empty[ClassificationResult]

  override def receiveRecover: Receive = Actor.emptyBehavior

  override def receiveCommand: Receive = {
    case SnapshotOffer(_, offeredSnapshot: List[ClassificationResult @unchecked]) ⇒
      exercises = offeredSnapshot

    case ClassifiedExercise(userId, result) ⇒
      log.info(s"ClassifiedExercise for $userId in AS ${self.path.toString}")
      self ! result

    case e@ClassificationResult(confidence, exercise) ⇒
      log.info(s"ClassificationResult in AS ${self.path.toString}")
      persist(e) { evt ⇒
        if (confidence > 0.0) {
          exercises = e :: exercises
          //exercise.foreach(e => UserPushNotification.lookup ! DefaultMessage(e, Some(1), Some("default")))
        }
        log.info(s"Now with ${exercises.size} exercises")
      }

    // query for exercises
    case GetExercises =>
      log.info(s"GetExercises in AS ${self.path.toString}")
      sender() ! exercises
  }

  override def persistenceId: String = s"user-exercise-${self.path.name}"
}
