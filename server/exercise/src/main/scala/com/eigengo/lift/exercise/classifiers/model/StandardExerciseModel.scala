package com.eigengo.lift.exercise.classifiers.model

import akka.actor.ActorLogging
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.ExerciseModel.Query
import com.eigengo.lift.exercise.classifiers.workflows.{ClassificationAssertions, GestureWorkflows}
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel

/**
 * Gesture classification model.
 *
 * Essentially, we view our model traces as being streams here. As a result, all queries are evaluated (on the actual
 * stream) from the time point they are received by the model.
 */
abstract class StandardExerciseModel(val sessionProps: SessionProperties, toWatch: Set[Query] = Set.empty)
  extends ExerciseModel
  with StandardEvaluation
  with GestureWorkflows
  with ActorLogging {

  this: SMTInterface =>

  import ClassificationAssertions._
  import FlowGraphImplicits._

  val name = "tap"

  for (query <- toWatch) {
    watch += (query -> query)
  }

  /**
   * Monitor wrist sensor and add in tap gesture detection.
   */
  val workflow = {
    val in = UndefinedSource[SensorNetValue]
    val out = UndefinedSink[BindToSensors]

    PartialFlowGraph { implicit builder =>
      val classifier = IdentifyGestureEvents()
      val split = Broadcast[SensorNetValue]
      val merge = Zip[Set[Fact], SensorNetValue]

      in ~> split

      split ~> Flow[SensorNetValue].map(_.toMap(SensorDataSourceLocationWrist).asInstanceOf[AccelerometerValue]).via(classifier.map(_.toSet)) ~> merge.left

      split ~> Flow[SensorNetValue] ~> merge.right

      merge.out ~> Flow[(Set[Fact], SensorNetValue)].map { case (facts, data) => BindToSensors(facts, Set(), Set(), Set(), Set(), data) } ~> out
    }.toFlow(in, out)
  }

}
