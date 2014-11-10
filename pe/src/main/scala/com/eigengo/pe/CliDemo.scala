package com.eigengo.pe

import java.util.UUID

import akka.actor.ActorSystem
import scodec.bits.BitVector

trait CliDemo {
  import exercise.Exercise._
  def system: ActorSystem
  lazy val arm3 = BitVector.fromInputStream(getClass.getResourceAsStream("/arm3.dat"))

  def demo(): Unit = {
    system.actorSelection("/user/exercise") ! ExerciseDataCmd(UUID.randomUUID(), arm3)
  }
}
