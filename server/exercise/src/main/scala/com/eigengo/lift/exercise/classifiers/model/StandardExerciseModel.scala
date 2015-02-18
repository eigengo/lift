package com.eigengo.lift.exercise.classifiers.model

import akka.actor.ActorLogging
import akka.stream.OverflowStrategy
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
abstract class StandardExerciseModel(sessionProps: SessionProperties, tapSensor: SensorDataSourceLocation, toWatch: Set[Query] = Set.empty)
  extends ExerciseModel("tap", sessionProps, toWatch)
  with StandardEvaluation
  with ActorLogging {

  this: SMTInterface =>

  import ClassificationAssertions._
  import FlowGraphImplicits._

  // Workflow for recognising 'tap' gestures that are detected via `tapSensor`
  object Tap extends GestureWorkflows("tap", context.system.settings.config)

  /**
   * Monitor wrist sensor and add in tap gesture detection.
   */
  val workflow = {
    val in = UndefinedSource[SensorNetValue]
    val out = UndefinedSink[BindToSensors]

    PartialFlowGraph { implicit builder =>
      val classifier = Tap.identifyEvent
      val split = Broadcast[SensorNetValue]
      val merge = Zip[Set[Fact], SensorNetValue]

      in ~> split

      split ~> Flow[SensorNetValue]
        .mapConcat(_.toMap(tapSensor).find(_.isInstanceOf[AccelerometerValue]).asInstanceOf[Option[AccelerometerValue]].toList)
        .via(classifier.map(_.toSet)) ~> merge.left

      split ~> merge.right

      merge.out ~> Flow[(Set[Fact], SensorNetValue)].map { case (facts, data) => BindToSensors(facts, Set(), Set(), Set(), Set(), data) } ~> out
    }.toFlow(in, out)
  }

}
