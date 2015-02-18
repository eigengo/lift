package com.eigengo.lift.exercise

import java.util.Date

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, ImplicitSender, TestKitBase}
import com.eigengo.lift.common.UserId
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpec}

class UserExercisesTracingTest extends FlatSpec with TestKitBase with Matchers with ImplicitSender {
  import UserExercises._
  override implicit lazy val system: ActorSystem = ActorSystem("test", ConfigFactory.load("test.conf"))
  val tracer = TestActorRef(new UserExercisesTracing(UserId.randomId().toString))

  "The session events" should "be saved" in {
    val id = SessionId.randomId()
    val props = SessionProperties(new Date(), Seq(), 1.0)
    val ad = AccelerometerData(100, List(AccelerometerValue(1, 3, 4)))
    val sdwl = SensorDataWithLocation(SensorDataSourceLocationAny, List(ad))

    tracer ! SessionStartedEvt(id, props)

    tracer ! ExerciseEvt(id, ModelMetadata.user, Exercise("user", Some(1.0), None))
    tracer ! ClassifyExerciseEvt(props, List(sdwl))
    tracer ! ClassifyExerciseEvt(props, List(sdwl))

    tracer ! SessionEndedEvt(id)
  }
}
