package com.eigengo.lift.exercise

import akka.actor.{ActorRef, ActorSystem, Actor, Props}
import akka.event.LoggingReceive
import akka.testkit.{TestKit, TestProbe, TestActorRef}
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.typesafe.config.ConfigFactory
import java.text.SimpleDateFormat
import org.scalatest._
import org.scalatest.prop._

class UserExercisesClassifierTest
  extends TestKit(ActorSystem("TestSystem", ConfigFactory.load("test.conf")))
  with PropSpecLike
  with PropertyChecks
  with Matchers
  with ExerciseGenerators {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val startDate = dateFormat.parse("1970-01-01")
  val sessionProps = SessionProperties(startDate, Seq("Legs"), 1.0)

  class DummyModel(probe: ActorRef) extends Actor {
    def receive = LoggingReceive {
      case event: SensorNet =>
        probe ! event
    }
  }

  property("UserExercisesClassifier should correctly 'slice up' ClassifyExerciseEvt into SensorNet events") {
    val width = 2//0
    val height = 3//0

    forAll(ClassifyExerciseEvtGen(width, height)) { (event: ClassifyExerciseEvt) =>
      val modelProbe = TestProbe()
      val classifier = TestActorRef(new UserExercisesClassifier(sessionProps, Props(new DummyModel(modelProbe.ref))))

      classifier ! event

      val msgs = modelProbe.receiveN(width).asInstanceOf[Vector[SensorNet]].toList
      for (result <- msgs) {
        assert(result.toMap.values.forall(_.values.length == height))
      }
      for (sensor <- Sensor.sourceLocations) {
        assert(msgs.flatMap(_.toMap(sensor).values) == event.sensorData.find(_.location == sensor).get.data.flatMap(_.values))
      }
    }
  }

}
