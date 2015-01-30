package com.eigengo.lift.exercise.classifiers.model

import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.workflows.{ClassificationAssertions, GestureWorkflows}
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.eigengo.lift.exercise.classifiers.ExerciseModel

/**
 * Assumptions:
 *   1. `AccelerometerData` sample rate is fixed to a known constant value for all sensors
 *   2. `ClassifyExerciseEvt` are received in the correct temporal order
 *   3. for each member of `Sensor.sourceLocations`, there is a unique and corresponding member in the sensor data for
 *      the `ClassifyExerciseEvt` instance
 *
 * Essentially, we view our model traces as being streams here. As a result, all queries are evaluated (on the actual
 * stream) from the time point they are received by the model.
 *
 * TODO: rewrite model so that these assumptions may be weakened further!
 */
class GestureClassificationModel extends ExerciseModel with GestureWorkflows {

  import ClassificationAssertions._
  import ExerciseModel._

  val name = "gesture"
  val config = ??? // TODO:

  type Result = Any // TODO:

  // Here we only monitor wrist locations for recognisable gestures
  private val classifier = GestureClassification(Set(SensorDataSourceLocationWrist), Sensor.sourceLocations)

  // FIXME: this can't work - starting with an empty stream means we'll complete the moment a sink connects (and nothing has been added into the stream)!!
  private var in: Map[SensorDataSourceLocation, Source[AccelerometerValue]] = Sensor.sourceLocations.map(loc => (loc, Source.empty())).toMap

  FlowGraph { implicit builder =>
    builder.importPartialFlowGraph(classifier.graph)

    builder.attachSource(classifier.inputTap(SensorDataSourceLocationWrist), in(SensorDataSourceLocationWrist))

    for (loc <- Sensor.sourceLocations) {
      if (loc == SensorDataSourceLocationWrist) {
        builder.connect(classifier.outputTap(SensorDataSourceLocationWrist), Flow[AccelerometerValue], classifier.inputModulate(loc))
      } else {
        builder.attachSource(classifier.inputModulate(loc), in(loc))
      }
    }

    for (loc <- Sensor.sourceLocations) {
      builder.attachSink(classifier.outputModulate(loc), Sink.foreach { assertion =>
        // TODO: should evaluate currently active queries for each new assertion that we receive
      })
    }
  }.run()

  def update[A <: SensorData](sdwls: ClassifyExerciseEvt[A]) = {
    sdwls.sensorData.flatMap { sdwl =>
      sdwl.data.map(av => (sdwl.location, av))
    }.groupBy(_._1)
    .mapValues(_.map(_._2))
    .foreach { case (loc, avs) =>
      // TODO: need to correctly 'butt' this into *in* workflow
      in = in + (loc -> in(loc).concat(Source(avs)))
    }
  }

  def evaluate(query: Query) = query match {
    case True() =>

    case Box(subQuery) =>

    case Diamond(subQuery) =>

  }

}
