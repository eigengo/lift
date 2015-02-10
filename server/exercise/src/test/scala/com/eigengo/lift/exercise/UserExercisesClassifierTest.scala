package com.eigengo.lift.exercise

import java.util.Date

import akka.actor.Actor
import akka.actor.Props
import akka.testkit.{TestProbe, TestActorRef}
import java.text.SimpleDateFormat
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
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

  def SensorDataWithLocationGen(location: SensorDataSourceLocation, width: Int, height: Int): Gen[SensorDataWithLocation] =
    for {
      data <- listOfN(width, SensorDataGen(height))
    } yield SensorDataWithLocation(location, data)

  val SessionPropertiesGen: Gen[SessionProperties] =
    for {
      date <- arbitrary[Long].map(t => new Date(t))
      group <- listOf(arbitrary[String])
      intensity <- Gen.choose[Double](0, 1) suchThat (_ > 0)
    } yield SessionProperties(date, group, intensity)

  def ClassifyExerciseEvtGen(size: Int, width: Int, height: Int): Gen[ClassifyExerciseEvt] =
    for {
      sessionProps <- SessionPropertiesGen
      events <- mapOf(Sensor.sourceLocations.map(l => SensorDataWithLocationGen(l, width, height))) // FIXME:
    } yield ClassifyExerciseEvt(sessionProps, events)

}

class UserExercisesClassifierTest
  extends PropSpec
  with PropertyChecks
  with Matchers
  with ExerciseGenerators {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val startDate = dateFormat.parse("1970-01-01")
  val sessionProps = SessionProperties(startDate, Seq("Legs"), 1.0)

  class DummyModel(probe: TestProbe) extends Actor {
    def receive = {
      case event: SensorNet =>
        probe.ref ! event
    }
  }

  property("") {
    forAll(ClassifyExerciseEvtGen(10, 20, 30)) { (event: ClassifyExerciseEvt) =>
      val modelProbe = TestProbe()
      val classifier = TestActorRef(new UserExercisesClassifier(sessionProps, Props(new DummyModel(modelProbe)))).underlyingActor

      classifier.receive(event)

      for (block <- 0 to 10) {
        val result = modelProbe.expectMsgType[SensorNet]
        // TODO: add in expectations on result
      }
    }
  }

}
