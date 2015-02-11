package com.eigengo.lift.exercise

import akka.actor.{ActorRef, ActorSystem, Actor, Props}
import akka.event.LoggingReceive
import akka.testkit.{TestKit, TestProbe, TestActorRef}
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.typesafe.config.ConfigFactory
import java.text.SimpleDateFormat
import java.util.Date
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest._
import org.scalatest.prop._

trait ExerciseGenerators {

  val SensorValueGen: Gen[AccelerometerValue] =
    for {
      x <- arbitrary[Int]
      y <- arbitrary[Int]
      z <- arbitrary[Int]
    } yield AccelerometerValue(x, y, z)

  def SensorDataGen(size: Int): Gen[SensorData] =
    for {
      rate <- Gen.posNum[Int]
      data <- listOfN(size, SensorValueGen)
    } yield new SensorData {
        def samplingRate = rate
        def values = data
      }

  val SensorDataSourceLocationGen: Gen[SensorDataSourceLocation] =
    Gen.oneOf(SensorDataSourceLocationWrist, SensorDataSourceLocationWaist, SensorDataSourceLocationFoot, SensorDataSourceLocationChest, SensorDataSourceLocationAny)

  def SensorDataWithLocationGen(width: Int, height: Int): Gen[SensorDataWithLocation] =
    for {
      location <- SensorDataSourceLocationGen
      data <- listOfN(width, SensorDataGen(height))
    } yield SensorDataWithLocation(location, data)

  val SessionPropertiesGen: Gen[SessionProperties] =
    for {
      date <- arbitrary[Long].map(t => new Date(t))
      group <- listOf(arbitrary[String])
      intensity <- Gen.choose[Double](0, 1) suchThat (_ > 0)
    } yield SessionProperties(date, group, intensity)

  // Generator ensures that `sensorData` list has information for each known sensor
  def ClassifyExerciseEvtGen(width: Int, height: Int): Gen[ClassifyExerciseEvt] =
    for {
      sessionProps <- SessionPropertiesGen
      events <- listOfN(Sensor.sourceLocations.size, SensorDataWithLocationGen(width, height)).map(_.zipWithIndex.map { case (sdwl, n) => sdwl.copy(location = Sensor.sourceLocations.toList(n)) })
    } yield ClassifyExerciseEvt(sessionProps, events)

}

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
    val width = 20
    val height = 30

    forAll(ClassifyExerciseEvtGen(width, height)) { (event: ClassifyExerciseEvt) =>
      val modelProbe = TestProbe()
      val classifier = TestActorRef(new UserExercisesClassifier(sessionProps, Props(new DummyModel(modelProbe.ref))))

      classifier ! event

      val msgs = modelProbe.receiveN(width).asInstanceOf[Vector[SensorNet]]
      for (result <- msgs) {
        assert(result.toMap.values.forall(_.values.length == height))
      }
    }
  }

}
