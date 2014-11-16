package com.eigengo.lift.exercise

import akka.actor.ActorRefFactory
import akka.testkit.TestKitBase
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.BitVector
import spray.testkit.ScalatestRouteTest

class UserExercisesExerciseDataProcessorServiceTest
  extends FlatSpec with ScalatestRouteTest with TestKitBase with Matchers
  with ExerciseService with ExerciseMarshallers with LiftTestMarshallers {
  val route = exerciseRoute(testActor, testActor)

  def actorRefFactory: ActorRefFactory = system

  def getResourceBitVector(resourceName: String): BitVector = {
    val is = getClass.getResourceAsStream(resourceName)
    BitVector.fromInputStream(is)
  }

  "ExerciseProcessor" should "accept requests" in {
    val bv = getResourceBitVector("/training/arm3.dat")
    Post("/exercise/C753CD2F-A46E-4C1E-9856-26C78FFAC760", bv) ~> route ~> check {
      responseAs[String] === "OK"
    }
  }

}
