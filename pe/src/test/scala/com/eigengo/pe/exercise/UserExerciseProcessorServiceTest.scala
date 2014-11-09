package com.eigengo.pe.exercise

import akka.actor.ActorRefFactory
import com.eigengo.pe.{LiftMarshallers, LiftTestMarshallers}
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.BitVector
import spray.testkit.ScalatestRouteTest

class UserExerciseProcessorServiceTest
  extends FlatSpec with ScalatestRouteTest with Matchers
  with UserExerciseProcessorService with LiftMarshallers with LiftTestMarshallers {

  def actorRefFactory: ActorRefFactory = system

  def getResourceBitVector(resourceName: String): BitVector = {
    val is = getClass.getResourceAsStream(resourceName)
    val bv = BitVector.fromInputStream(is)
    is.close()
    bv
  }

  "UserExerciseProcessor" should "accept requests" in {
    val bv = getResourceBitVector("/training/arm3.dat")
    Post("/exercise", bv) ~> userExerciseProcessorRoute ~> check {
      responseAs[String] === "OK"
    }
  }

}
