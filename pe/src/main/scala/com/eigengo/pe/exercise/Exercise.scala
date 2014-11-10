package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.ClusterSharding
import scodec.bits.BitVector

object Exercise {

  sealed trait Command {
    def userId: UUID
  }
  final case class ExerciseDataCmd(userId: UUID, bits: BitVector) extends Command

}

class Exercise extends Actor with ActorLogging {
  import com.eigengo.pe.exercise.Exercise._
  val userExerciseRegion = ClusterSharding(context.system).shardRegion(UserExercise.shardName)

  override def receive: Receive = {
    case e@ExerciseDataCmd(_, _) ⇒ userExerciseRegion ! e
  }
}