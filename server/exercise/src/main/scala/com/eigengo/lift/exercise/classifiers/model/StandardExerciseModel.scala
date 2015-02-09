package com.eigengo.lift.exercise.classifiers.model

import akka.actor.ActorLogging
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.ExerciseModel.Query
import com.eigengo.lift.exercise.classifiers.workflows.{ClassificationAssertions, GestureWorkflows}
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel

trait StandardEvaluation {

  import ClassificationAssertions._
  import ExerciseModel._

  // TODO: introduce memoisation into `evaluate` functions

  // TODO: introduce use of SMT library (e.g. ScalaZ3 or Scala SMT-LIB)?
  
  def evaluateAtSensor(fact: Fact, sensor: SensorQuery)(state: BindToSensors): Boolean = sensor match {
    case AllSensors =>
      state.toMap.values.forall(_.contains(fact))

    case SomeSensor =>
      state.toMap.values.exists(_.contains(fact))

    case NamedSensor(location) =>
      state.toMap(location).contains(fact)
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

  def evaluate(query: Query)(state: BindToSensors, lastState: Boolean): QueryValue = query match {
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
  extends ExerciseModel
  with StandardEvaluation
  with GestureWorkflows
  with ActorLogging {

  import ClassificationAssertions._
  import FlowGraphImplicits._

  val name = "gesture"
  val samplingRate = config.getInt("classification.frequency")

  private val classifier = IdentifyGestureEvents()

  val workflow = {
    val in = UndefinedSource[SensorNetValue]
    val out = UndefinedSink[BindToSensors]

    PartialFlowGraph { implicit builder =>
      val inConnectNode = UndefinedSink[SensorValue]
      val outConnectNode = UndefinedSource[Set[Fact]]
      val split = Broadcast[SensorNetValue]
      val merge = Zip[Set[Fact], SensorNetValue]

      builder.importPartialFlowGraph(classifier.graph)

      builder.connect(inConnectNode, Flow[AccelerometerValue], classifier.in)
      builder.connect(classifier.out, Flow[Option[Fact]].map(_.toSet), outConnectNode)

      in ~> split

      split ~> Flow[SensorNetValue].map(_.toMap(SensorDataSourceLocationWrist)) ~> inConnectNode
      outConnectNode ~> merge.left

      split ~> Flow[SensorNetValue] ~> merge.right

      merge.out ~> Flow[(Set[Fact], SensorNetValue)].map { case (facts, data) => BindToSensors(facts, Set(), Set(), Set(), Set(), data) } ~> out
    }.toFlow(in, out)
  }

  /**
   * We use `aroundReceive` here to transform each `SensorNet` message into a list of `SensorNetValue` messages.
   */
  override def aroundReceive(receive: Receive, msg: Any) = msg match {
    // TODO: refactor code so that the following assumptions may be weakened further!
    case event: SensorNet =>
      require(
        event.toMap.values.forall(_.values.nonEmpty),
        "all sensors in a network should produce some sensor value"
      )
      val blockSize = event.toMap.values.head.values.length
      require(
        event.toMap.values.forall(_.values.length == blockSize),
        "all sensors in a network produce the same number of sensor values"
      )
      require(
        event.toMap.values.forall(_.samplingRate == samplingRate),
        "all sensors have a fixed known sample rate"
      )

      val sensorEvents = (0 until blockSize).map(block => SensorNetValue(event.toMap.mapValues(_.values(block))))

      for (event <- sensorEvents) {
        super.aroundReceive(receive, event)
      }

    case _ =>
      super.aroundReceive(receive, msg)
  }

}
