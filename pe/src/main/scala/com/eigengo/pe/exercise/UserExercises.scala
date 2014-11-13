package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{ActorLogging, ActorRefFactory, Props}
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.eigengo.pe.push.UserPushNotification
import com.eigengo.pe.push.UserPushNotification.DefaultMessage
import com.eigengo.pe.{AccelerometerData, actors}

object UserExercises {
  import com.eigengo.pe.exercise.ExerciseProcessor._
  val shardName = "user-exercises-shard"
  val props = Props[UserExercises]
  def lookup(implicit arf: ActorRefFactory) = actors.shard.lookup(arf, shardName)

  case class GetUserExercises(userId: UUID)
  case object GetExercises

  val idExtractor: ShardRegion.IdExtractor = {
    case ExerciseDataEvt(userId, data) ⇒ (userId.toString, data)
    case GetUserExercises(userId) ⇒ (userId.toString, GetExercises)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case GetUserExercises(userId) ⇒ userId.toString
    case ExerciseDataEvt(userId, _) ⇒ userId.toString
  }

}

class UserExercises extends PersistentActor with ActorLogging {
  import com.eigengo.pe.exercise.ExerciseClassifier._
  import com.eigengo.pe.exercise.UserExercises._
  private var exercises = List.empty[ClassifiedExercise]
  private val userId: UUID = UUID.fromString(self.path.name)
  override val persistenceId: String = s"user-exercises-${self.path.name}"

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, offeredSnapshot: List[ClassifiedExercise @unchecked]) ⇒
      exercises = offeredSnapshot
    case RecoveryCompleted ⇒
    case ad@AccelerometerData(_, _) ⇒
      ExerciseClassifiers.lookup ! ad

  }

  override def receiveCommand: Receive = {
    case evt@AccelerometerData(_, _) ⇒
      log.info(s"AccelerometerData in AS ${self.path.toString}")
      persist(evt) { ad ⇒ ExerciseClassifiers.lookup ! ad }

    case e@ClassifiedExercise(confidence, exercise) ⇒
      log.info(s"ClassificationResult in AS ${self.path.toString}")
      if (confidence > 0.0) {
        exercises = e :: exercises
        exercise.foreach(e => UserPushNotification.lookup ! DefaultMessage(userId, e, Some(1), Some("default")))
      }
      saveSnapshot(exercises)
      log.info(s"Now with ${exercises.size} exercises")

    // query for exercises
    case GetExercises =>
      log.info(s"GetExercises in AS ${self.path.toString}")
      sender() ! exercises
  }

}
