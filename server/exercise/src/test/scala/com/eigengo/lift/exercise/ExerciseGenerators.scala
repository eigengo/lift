package com.eigengo.lift.exercise

import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import java.util.Date
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._

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

  def SensorNetGen(size: Int): Gen[SensorNet] =
    for {
      sensorMap <- listOfN(Sensor.sourceLocations.size, SensorDataGen(size)).map(_.zipWithIndex.map { case (sv, n) => (Sensor.sourceLocations.toList(n), sv) }.toMap[SensorDataSourceLocation, SensorData])
    } yield SensorNet(sensorMap)

  val SensorNetValueGen: Gen[SensorNetValue] =
    for {
      sensorMap <- listOfN(Sensor.sourceLocations.size, SensorValueGen).map(_.zipWithIndex.map { case (sv, n) => (Sensor.sourceLocations.toList(n), sv) }.toMap[SensorDataSourceLocation, SensorValue])
    } yield SensorNetValue(sensorMap)

}
