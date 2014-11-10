package com.eigengo.pe

import java.util.UUID

import akka.actor.ActorSystem
import scodec.bits.BitVector

object CliDemo {
  import exercise.Exercise._
  lazy val arm3 = BitVector.fromInputStream(getClass.getResourceAsStream("/arm3.dat"))

  def demo(implicit system: ActorSystem): Unit = {
    system.actorSelection("/user/exercise") ! ExerciseDataCmd(UUID.randomUUID(), arm3)
  }
}
