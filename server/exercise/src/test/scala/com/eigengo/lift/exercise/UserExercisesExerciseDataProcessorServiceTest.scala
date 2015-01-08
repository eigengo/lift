package com.eigengo.lift.exercise

import akka.actor.ActorRefFactory
import akka.testkit.{ImplicitSender, TestKitBase, TestProbe}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.exercise.UserExercises.UserExerciseDataProcessSinglePacket
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.BitVector
import spray.testkit.ScalatestRouteTest

import scalaz.\/

class UserExercisesExerciseDataProcessorServiceTest
  extends FlatSpec with ScalatestRouteTest with TestKitBase with Matchers
  with ExerciseService with ExerciseMarshallers with LiftTestMarshallers with ImplicitSender {
  val probe = TestProbe()
  val route = exerciseRoute(probe.ref, testActor, testActor)

  override def testConfig: Config = ConfigFactory.load("/test.conf")

  def actorRefFactory: ActorRefFactory = system

  def getResourceBitVector(resourceName: String): BitVector = {
    val is = getClass.getResourceAsStream(resourceName)
    BitVector.fromInputStream(is)
  }

  "ExerciseProcessor" should "accept requests" in {
    val bv = getResourceBitVector("/measured/bicep-1/all.dat")
    val userId = "C753CD2F-A46E-4C1E-9856-26C78FFAC760"
    val sessionId = "C753CD2F-A46E-4C1E-9856-26C78FFAC7AA"
    Put(s"/exercise/$userId/$sessionId", bv) ~> route ~> check {
      //TODO: Fixme
      //responseAs[String] === "OK"
    }
    probe.expectMsg(UserExerciseDataProcessSinglePacket(UserId(userId), SessionId(sessionId), bv))
    probe.reply(\/.right("OK"))
  }

}
