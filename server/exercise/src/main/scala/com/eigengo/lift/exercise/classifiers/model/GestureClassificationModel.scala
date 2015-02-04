package com.eigengo.lift.exercise.classifiers.model

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.ExerciseModel.Query
import com.eigengo.lift.exercise.classifiers.workflows.{ClassificationAssertions, GestureWorkflows}
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel

/**
 * "Standard" model.
 *
 * Assumptions:
 *   1. `AccelerometerData` sample rate is fixed to a known constant value for all sensors
 *   2. `ClassifyExerciseEvt` are received in the correct temporal order
 *   3. for each member of `Sensor.sourceLocations`, there is a unique and corresponding member in the sensor data for
 *      the `ClassifyExerciseEvt` instance
 *   4. all members of `Sensor.sourceLocations`, have the same length `AccelerometerValue` (flattened) lists
 *
 * Essentially, we view our model traces as being streams here. As a result, all queries are evaluated (on the actual
 * stream) from the time point they are received by the model.
 *
 * TODO: rewrite model so that these assumptions may be weakened further!
 *//*
class GestureClassificationModel(val sessionProps: SessionProperties, val watch: Set[Query])
  extends ExerciseModel
  with GestureWorkflows
  with ActorPublisher
  with ActorLogging {

  import ClassificationAssertions._
  import ExerciseModel._

  val name = "gesture"
  val config = context.system.settings.config

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
        // TODO: group output into a Source[Map[SensorDataSourceLocation, Bind[AccelerometerValue]]]
        // TODO: should evaluate currently active queries for each new assertion that we receive
      })
    }
  }.run()

  override def receive = {
    case Update(sdwls) => {
      require(
        Sensor.sourceLocations.nonEmpty,
        "`Sensor.sourceLocations` is non empty"
      )
      require(
      {
        val n = sdwls.sensorData.head.data.asInstanceOf[List[AccelerometerData]].head.samplingRate
        Sensor.sourceLocations.forall(sl => sdwls.sensorData.exists(sdwl => sdwl.location == sl && sdwl.data.asInstanceOf[List[AccelerometerData]].forall(_.samplingRate == n)))
      },
      "`AccelerometerData` sample rate should be fixed to a known constant value for all sensors"
      )
      require(
        sdwls.sensorData.map(_.location).toSet == Sensor.sourceLocations && sdwls.sensorData.map(_.location).size == Sensor.sourceLocations.size,
        "for each member of `Sensor.sourceLocations`, there is a unique and corresponding member in the sensor data for the `ClassifyExerciseEvt` instance"
      )
      require(
      {
        val n = sdwls.sensorData.head.data.asInstanceOf[List[AccelerometerData]].flatMap(_.values).length
        Sensor.sourceLocations.forall(sl => sdwls.sensorData.exists(sdwl => sdwl.location == sl && sdwl.data.asInstanceOf[List[AccelerometerData]].flatMap(_.values).length == n))
      },
      "all members of `Sensor.sourceLocations`, have the same length `AccelerometerValue` (flattened) lists"
      )

      sdwls.sensorData.flatMap { sdwl =>
        sdwl.data.map(av => (sdwl.location, av))
      }.groupBy(_._1)
        .mapValues(_.map(_._2))
        .foreach { case (loc, avs) =>
        // TODO: need to correctly 'butt' this into *in* workflow
        in = in + (loc -> in(loc).concat(Source(avs)))
      }

      super.receive(sdwls)
    }
  }

  // TODO: introduce memoisation
  def evaluate[A <: SensorData](q: Query)(state: Bind[A], lastState: Boolean): QueryValue = q match {
    case Formula(assertion) =>
      StableValue(result = ???) // TODO: propositional case - only need to use current here

    case TT =>
      StableValue(result = true)

    case FF =>
      StableValue(result = false)

    case And(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => evaluate(q)(state, lastState))
      results.fold(StableValue(result = true))(meet)

    case Or(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => evaluate(q)(state, lastState))
      results.fold(UnstableValue(result = false))(join)

    case Exists(Assert(assertion), query1) =>
      (!lastState, evaluate(Formula(assertion))(state, lastState)) match {
        case (true, StableValue(true)) =>
          // Future evaluation of query1 will determine validity or otherwise of q
          UnstableValue(result = true) // TODO: schedule query1 for checking in next state

        case (false, _) =>
          StableValue(result = false)

        case (_, StableValue(false)) =>
          StableValue(result = false)

        case (true, value @ UnstableValue(_)) =>
          val errMsg = s"$assertion (which is propositional) did not evaluate to a stable value - evaluated to $value"
          log.error(errMsg)
          sys.error(errMsg)
      }

    case Exists(Test(query1), query2) =>
      meet(evaluate(query1)(state, lastState), evaluate(query2)(state, lastState))

    case Exists(Choice(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(Or(Exists(path1, query1), Exists(path2, query1), remainingPaths.map(p => Exists(p, query1)): _*))(state, lastState)

    case Exists(Seq(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(Exists(path1, Exists(path2, remainingPaths.foldLeft(query1) { case (q, p) => Exists(p, q) })))(state, lastState)

    case Exists(Repeat(path), query1) if lastState || testOnly(path) =>
      evaluate(query1)(state, lastState)

    case Exists(Repeat(path), query1) if !lastState =>
      join(
        evaluate(query1)(state, lastState),
        evaluate(Exists(path, Exists(Repeat(path), query1)))(state, lastState)
      )

      // TODO: not correct!
    case All(Assert(assertion), query1) =>
      (!lastState, evaluate(Formula(assertion))(state, lastState)) match {
        case (true, StableValue(true)) =>
          // Future evaluation of query1 will determine validity or otherwise of q
          UnstableValue(result = true) // TODO: schedule query1 for checking in next state

        case (false, _) =>
          StableValue(result = false)

        case (_, StableValue(false)) =>
          StableValue(result = false)

        case (true, value @ UnstableValue(_)) =>
          val errMsg = s"$assertion (which is propositional) did not evaluate to a stable value - evaluated to $value"
          log.error(errMsg)
          sys.error(errMsg)
      }

    case All(Test(query1), query2) =>
      join(evaluate(not(query1))(state, lastState), evaluate(query2)(state, lastState))

    case All(Choice(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(And(All(path1, query1), All(path2, query1), remainingPaths.map(p => All(p, query1)): _*))(state, lastState)

    case All(Seq(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(All(path1, All(path2, remainingPaths.foldLeft(query1) { case (q, p) => All(p, q) })))(state, lastState)


    case All(Repeat(path), query1) if lastState || testOnly(path) =>
      evaluate(query1)(state, lastState)

    case All(Repeat(path), query1) if !lastState =>
      meet(
        evaluate(query1)(state, lastState),
        evaluate(All(path, Exists(Repeat(path), query1)))(state, lastState)
      )
  }

}
*/