package com.eigengo.pe.exercise

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.testkit.TestKit
import com.eigengo.pe.{LiftTestMarshallers, LiftMarshallers}
import org.scalatest.{FlatSpecLike, Matchers}
import scodec.bits.BitVector
import spray.testkit.ScalatestRouteTest

class UserExerciseProcessorServiceTest
  extends TestKit(ActorSystem()) with ScalatestRouteTest with FlatSpecLike with Matchers
  with UserExerciseProcessorService with LiftMarshallers with LiftTestMarshallers {

  override implicit def actorRefFactory: ActorRefFactory = system

  def getResourceBitVector(resourceName: String): BitVector = {
    val is = getClass.getResourceAsStream(resourceName)
    val bv = BitVector.fromInputStream(is)
    is.close()
    bv
  }


  "UserExerciseProcessor" should "accept requests" in {
    val bv = getResourceBitVector("/training/arm3.dat")
    Post("/exercise", bv) ~> userExerciseProcessorRoute ~> check {
      responseAs[String] should be("OK")
    }
  }

}
