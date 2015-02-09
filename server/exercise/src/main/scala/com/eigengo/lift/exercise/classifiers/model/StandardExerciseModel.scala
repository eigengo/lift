package com.eigengo.lift.exercise.classifiers.model

import akka.actor.ActorLogging
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.ExerciseModel.Query
import com.eigengo.lift.exercise.classifiers.workflows.{ZipSet, ClassificationAssertions, GestureWorkflows}
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel

trait StandardEvaluation[A <: SensorData] {

  import ClassificationAssertions._
  import ExerciseModel._

  // TODO: introduce memoisation into `evaluate` functions

  // TODO: introduce use of SMT library (e.g. ScalaZ3 or Scala SMT-LIB)?
  
  def evaluateAtSensor(fact: Fact, sensor: SensorQuery)(state: Map[SensorDataSourceLocation, Bind[A]]): Boolean = sensor match {
    case AllSensors =>
      state.values.forall(_.assertion.contains(fact))

    case SomeSensor =>
      state.values.exists(_.assertion.contains(fact))

    case NamedSensor(location) =>
      state(location).assertion.contains(fact)
  }

  def emptyEvaluate(query: Query): QueryValue = query match {
    case Formula(_, _) =>
      StableValue(result = false)

    case TT =>
      StableValue(result = true)

    case FF =>
      StableValue(result = false)

    case And(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => emptyEvaluate(q))
      results.fold(StableValue(result = true))(meet)

    case Or(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => emptyEvaluate(q))
      results.fold(StableValue(result = false))(join)

    case Exists(Assert(_, _), _) =>
      StableValue(result = false)

    case Exists(Test(query1), query2) =>
      meet(emptyEvaluate(query1), emptyEvaluate(query2))

    case Exists(Choice(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(Or(Exists(path1, query1), Exists(path2, query1), remainingPaths.map(p => Exists(p, query1)): _*))

    case Exists(Seq(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(Exists(path1, Exists(path2, remainingPaths.foldLeft(query1) { case (q, p) => Exists(p, q) })))

    case Exists(Repeat(path), query1) =>
      emptyEvaluate(query1)

    case All(Assert(_, _), _) =>
      StableValue(result = true)

    case All(Test(query1), query2) =>
      join(emptyEvaluate(ExerciseModel.not(query1)), emptyEvaluate(query2))

    case All(Choice(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(And(All(path1, query1), All(path2, query1), remainingPaths.map(p => All(p, query1)): _*))

    case All(Seq(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(All(path1, All(path2, remainingPaths.foldLeft(query1) { case (q, p) => All(p, q) })))

    case All(Repeat(path), query1) =>
      emptyEvaluate(query1)
  }

  def evaluate(query: Query)(state: Map[SensorDataSourceLocation, Bind[A]], lastState: Boolean): QueryValue = query match {
    case Formula(sensor, fact) =>
      StableValue(result = evaluateAtSensor(fact, sensor)(state))

    case TT =>
      StableValue(result = true)

    case FF =>
      StableValue(result = false)

    case And(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => evaluate(q)(state, lastState))
      results.fold(StableValue(result = true))(meet)

    case Or(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => evaluate(q)(state, lastState))
      results.fold(StableValue(result = false))(join)

    case Exists(Assert(sensor, fact), query1) if !lastState && evaluateAtSensor(fact, sensor)(state) =>
      UnstableValue(result = true, query1) // FIXME: is result correct?

    case Exists(Assert(sensor, fact), query1) if lastState && evaluateAtSensor(fact, sensor)(state) =>
      emptyEvaluate(query1)

    case Exists(Assert(_, _), _) =>
      StableValue(result = false)

    case Exists(Test(query1), query2) =>
      meet(evaluate(query1)(state, lastState), evaluate(query2)(state, lastState))

    case Exists(Choice(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(Or(Exists(path1, query1), Exists(path2, query1), remainingPaths.map(p => Exists(p, query1)): _*))(state, lastState)

    case Exists(Seq(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(Exists(path1, Exists(path2, remainingPaths.foldLeft(query1) { case (q, p) => Exists(p, q) })))(state, lastState)

    case Exists(Repeat(path), query1) if testOnly(path) =>
      evaluate(query1)(state, lastState)

    case Exists(Repeat(path), query1) =>
      join(
        evaluate(query1)(state, lastState),
        evaluate(Exists(path, Exists(Repeat(path), query1)))(state, lastState)
      )

    case All(Assert(sensor, fact), query1) if !lastState && evaluateAtSensor(fact, sensor)(state) =>
      UnstableValue(result = true, query1) // FIXME: is result correct?

    case All(Assert(sensor, fact), query1) if lastState && evaluateAtSensor(fact, sensor)(state) =>
      emptyEvaluate(query1)

    case All(Assert(_, _), _) =>
      StableValue(result = true)

    case All(Test(query1), query2) =>
      join(evaluate(ExerciseModel.not(query1))(state, lastState), evaluate(query2)(state, lastState))

    case All(Choice(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(And(All(path1, query1), All(path2, query1), remainingPaths.map(p => All(p, query1)): _*))(state, lastState)

    case All(Seq(path1, path2, remainingPaths @ _*), query1) =>
      evaluate(All(path1, All(path2, remainingPaths.foldLeft(query1) { case (q, p) => All(p, q) })))(state, lastState)

    case All(Repeat(path), query1) if testOnly(path) =>
      evaluate(query1)(state, lastState)

    case All(Repeat(path), query1) =>
      meet(
        evaluate(query1)(state, lastState),
        evaluate(All(path, All(Repeat(path), query1)))(state, lastState)
      )
  }

}

/**
 * Gesture classification model.
 *
 * Essentially, we view our model traces as being streams here. As a result, all queries are evaluated (on the actual
 * stream) from the time point they are received by the model.
 */
class StandardExerciseModel(val sessionProps: SessionProperties, val negativeWatch: Set[Query] = Set.empty, val positiveWatch: Set[Query] = Set.empty)
  extends ExerciseModel[AccelerometerData]
  with StandardEvaluation[AccelerometerData]
  with GestureWorkflows
  with ActorLogging {

  import ClassificationAssertions._
  import ExerciseModel._
  import FlowGraphImplicits._

  val name = "gesture"
  val samplingRate = config.getInt("classification.frequency")

  // Here we only monitor wrist locations for recognisable gestures
  private val classifier = GestureClassification(Set[SensorDataSourceLocation](SensorDataSourceLocationWrist), Sensor.sourceLocations)

  /**
   * NOTE: `AccelerometerData` instances flowing here will have one `AccelerometerValue`
   */
  val workflow = {
    val in = UndefinedSource[Map[SensorDataSourceLocation, AccelerometerData]]
    val out = UndefinedSink[Map[SensorDataSourceLocation, Bind[AccelerometerData]]]

    PartialFlowGraph { implicit builder =>
      val split = Broadcast[Map[SensorDataSourceLocation, AccelerometerValue]]
      val merge = ZipSet[(SensorDataSourceLocation, Bind[AccelerometerData]), SensorDataSourceLocation](Sensor.sourceLocations)

      builder.importPartialFlowGraph(classifier.graph)

      in ~> Flow[Map[SensorDataSourceLocation, AccelerometerData]].map(_.mapValues(_.values.head)) ~> split

      for (loc <- Sensor.sourceLocations) {
        val connectNode = UndefinedSink[AccelerometerValue]

        if (loc == SensorDataSourceLocationWrist) {
          split ~> Flow[Map[SensorDataSourceLocation, AccelerometerValue]].map(_(loc)) ~> connectNode
          builder.connect(connectNode, Flow[AccelerometerValue], classifier.inputTap(SensorDataSourceLocationWrist))
          builder.connect(classifier.outputTap(SensorDataSourceLocationWrist), Flow[AccelerometerValue], classifier.inputModulate(loc))
        } else {
          split ~> Flow[Map[SensorDataSourceLocation, AccelerometerValue]].map(_(loc)) ~> connectNode
          builder.connect(connectNode, Flow[AccelerometerValue], classifier.inputModulate(loc))
        }
      }

      for (loc <- Sensor.sourceLocations) {
        val connectNode = UndefinedSource[Bind[AccelerometerValue]]

        builder.connect(classifier.outputModulate(loc), Flow[Bind[AccelerometerValue]], connectNode)
        connectNode ~> Flow[Bind[AccelerometerValue]].map {
          case Bind(assertion, value) =>
            (loc, Bind(assertion, AccelerometerData(samplingRate, List(value))))
        } ~> merge.in(loc)
      }

      merge.out ~> Flow[Set[(SensorDataSourceLocation, Bind[AccelerometerData])]].map(_.toMap) ~> out
    }.toFlow(in, out)
  }

  /**
   * We use `aroundReceive` here to transform each Update message into a list of Update messages. Essentially, there is
   * an update message per `AccelerometerValue` (sliced across each sensor net location).
   */
  override def aroundReceive(receive: Receive, msg: Any) = msg match {
    /**
     * Assumptions:
     *   1. all members of `Sensor.sourceLocations`, have at least one `AccelerometerValue`
     *   2. all members of `Sensor.sourceLocations`, have the same length `AccelerometerValue` lists
     *   3. `AccelerometerData` sample rate is fixed to a known constant value for all sensors
     *
     * TODO: rewrite so that these assumptions may be weakened further!
     */
    case Update(sensorMap: Map[SensorDataSourceLocation, AccelerometerData]) => {
      require(
        sensorMap.values.nonEmpty,
        "all members of `Sensor.sourceLocations`, have at least one `AccelerometerValue` value"
      )
      require(
        {
          val n = sensorMap.values.head.values.length
          Sensor.sourceLocations.forall(sl => sensorMap(sl).values.length == n)
        },
        "all members of `Sensor.sourceLocations`, have the same length `AccelerometerValue` lists"
      )
      require(
        Sensor.sourceLocations.forall(sl => sensorMap(sl).samplingRate == samplingRate),
        "`AccelerometerData` sample rate should be fixed to a known constant value for all sensors"
      )

      val eventSize = sensorMap.values.head.values.length
      val sensorEvents = (0 until eventSize).map(evt => Sensor.sourceLocations.map(sl => (sl, sensorMap(sl).copy(values = List(sensorMap(sl).values(evt))))).toMap)

      for (event <- sensorEvents) {
        super.aroundReceive(receive, event)
      }
    }

    case _ =>
      super.aroundReceive(receive, msg)
  }

}
