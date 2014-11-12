package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorLogging, ActorRefFactory, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentView, SnapshotOffer}
import com.eigengo.pe.actors

object UserExercise {
  import com.eigengo.pe.exercise.ExerciseClassifier._
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

class UserExercise extends PersistentView with ActorLogging {
  import com.eigengo.pe.exercise.ExerciseClassifier._
  import com.eigengo.pe.exercise.UserExercise._
  private var exercises = List.empty[ClassificationResult]

  override def viewId: String = s"user-exercise-view-${self.path.name}"
  override val persistenceId: String = s"user-exercise-${self.path.name}"

  override def receive: Receive = {
    case SnapshotOffer(_, offeredSnapshot: List[ClassificationResult @unchecked]) ⇒
      exercises = offeredSnapshot

    case ClassifiedExercise(userId, result) ⇒
      log.info(s"ClassifiedExercise for $userId in AS ${self.path.toString}")
      self ! result

    case e@ClassificationResult(confidence, exercise) ⇒
      log.info(s"ClassificationResult in AS ${self.path.toString}")
      if (confidence > 0.0) {
        exercises = e :: exercises
        //exercise.foreach(e => UserPushNotification.lookup ! DefaultMessage(e, Some(1), Some("default")))
      }
      saveSnapshot(exercises)
      log.info(s"Now with ${exercises.size} exercises")

    // query for exercises
    case GetExercises =>
      log.info(s"GetExercises in AS ${self.path.toString}")
      sender() ! exercises
  }

}
