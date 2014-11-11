package com.eigengo.pe.exercise

import akka.actor.ActorRefFactory
import akka.testkit.TestActorRef
import com.eigengo.pe.{LiftMarshallers, LiftTestMarshallers}
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.BitVector
import spray.testkit.ScalatestRouteTest

class ExerciseServiceTest
  extends FlatSpec with ScalatestRouteTest with Matchers
  with ExerciseService with LiftMarshallers with LiftTestMarshallers {

  def actorRefFactory: ActorRefFactory = system

  def getResourceBitVector(resourceName: String): BitVector = {
    val is = getClass.getResourceAsStream(resourceName)
    BitVector.fromInputStream(is)
  }

  "UserExerciseProcessor" should "accept requests" in {
    val bv = getResourceBitVector("/training/arm3.dat")
    Post("/exercise", bv) ~> userExerciseProcessorRoute ~> check {
      responseAs[String] === "OK"
    }
  }

}
