package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.scaladsl._
import breeze.linalg.DenseMatrix
import com.eigengo.lift.exercise.AccelerometerValue
import com.eigengo.lift.exercise.classifiers.svm.{SVMClassifier, SVMModelParser}
import com.typesafe.config.Config

/**
 * Trait that implements reactive stream components that can:
 *
 *   * [IdentifyGestureEvents] tap into sensor streams and generate fact events whenever a sample window is classified as a gesture
 *   * [MergeSignals]          merge collections of fact signals into a single fact event
 *   * [ModulateSensorNet]     modulate the signals (e.g. by binding them with facts) in a network of sensors
 */
trait GestureWorkflows extends SVMClassifier {

  import ClassificationAssertions._
  import FlowGraphImplicits._

  def name: String
  def config: Config

  lazy val frequency = {
    val value = config.getInt("classification.frequency")
    assert(value > 0)
    value
  }
  lazy val threshold = {
    val value = config.getDouble(s"classification.gesture.$name.threshold")
    assert(0 <= value && value <= 1)
    value
  }
  lazy val windowSize = {
    val value = config.getInt(s"classification.gesture.$name.size")
    assert(value > 0)
    value
  }

  // NOTE: here we accept throwing an exception in loading R libSVM models (since this indicates a catastrophic configuration error!)
  private lazy val model = new SVMModelParser(name)(config).model.get

  /**
   * Measures probability that sampled window is recognised as a gesture event.
   *
   * @param sample sampled data to be tested
   * @return       probability that a gesture was recognised in the sample window
   */
  private def probabilityOfGestureEvent(sample: List[AccelerometerValue]): Double = {
    require(sample.length == windowSize)

    val data = DenseMatrix(sample.map(v => (v.x.toDouble, v.y.toDouble, v.z.toDouble)): _*)

    predict(model, data, taylor_radial_kernel()).positiveMatch
  }

  /**
   * Flowgraph that taps the in-out stream and, if a gesture is recognised, sends a `Fact` message to the `tap` sink.
   */
  class IdentifyGestureEvents {
    val in = UndefinedSource[AccelerometerValue]
    val out = UndefinedSink[AccelerometerValue]
    val tap = UndefinedSink[Option[Fact]]

    val graph = PartialFlowGraph { implicit builder =>
      val split = Broadcast[AccelerometerValue]

      in ~> split ~> out
      split ~> Flow[AccelerometerValue].transform(() => SlidingWindow[AccelerometerValue](windowSize)).map { (sample: List[AccelerometerValue]) =>
        if (sample.length == windowSize) {
          // Saturated windows may be classified
          val matchProbability = probabilityOfGestureEvent(sample)

          if (matchProbability >= threshold) {
            Some(Gesture(name, threshold))
          } else {
            Some(NegGesture(name, threshold))
          }
        } else {
          // Truncated windows are never classified (these typically occur when the stream closes)
          None
        }
      } ~> tap
    }
  }

  object IdentifyGestureEvents {
    def apply() = new IdentifyGestureEvents()
  }

  /**
   * Flowgraph that merges (via a user supplied function) a collection of input sources into a single output sink.
   *
   * @param size number of input sources that we are to bundle and merge
   */
  class MergeSignals[A, B](size: Int, merge: Set[A] => B) {
    require(size > 0)

    val in = (0 until size).map(_ => UndefinedSource[A])
    val out = UndefinedSink[B]

    val graph = PartialFlowGraph { implicit builder =>
      // We separate out size 1 case since `ZipN` nodes need at least 2 upstream nodes
      if (size == 1) {
        in.head ~> Flow[A].map(v => merge(Set(v))) ~> out
      } else {
        val zip = ZipN[A](in.size)

        for ((probe, index) <- in.zipWithIndex) {
          probe ~> zip.in(index)
        }
        zip.out ~> Flow[Set[A]].map(merge) ~> out
      }
    }
  }

  object MergeSignals {
    def apply[A, B](size: Int)(merge: Set[A] => B) = {
      new MergeSignals[A, B](size, merge)
    }
  }

  /**
   * Flowgraph that modulates all sensors in a network of location tagged sensors. Messages on the `transform` source
   * determine how signals in the sensor net are modulated or transformed.
   *
   * @param locations set of locations that make up the sensor network
   */
  class ModulateSensorNet[A, B, L](locations: Set[L]) {
    require(locations.nonEmpty)

    val in = locations.map(loc => (loc, UndefinedSource[A])).toMap
    val transform = UndefinedSource[Option[Fact]]
    val out = locations.map(loc => (loc, UndefinedSink[Bind[A]])).toMap

    val graph = PartialFlowGraph { implicit builder =>
      // We separate out 1-element case since `Broadcast` nodes need at least 2 downstream nodes
      if (locations.size == 1) {
        val zip = ZipWith[A, Option[Fact], Bind[A]]((msg: A, tag: Option[Fact]) => tag match {
          case Some(fact) =>
            Bind(Set(fact), msg)

          case None =>
            Bind(Set.empty, msg)
        })

        in(locations.head) ~> zip.left
        transform ~> zip.right
        zip.out ~> out(locations.head)
      } else {
        val broadcast = Broadcast[Option[Fact]]

        transform ~> broadcast
        for ((location, sensor) <- in) {
          val zip = ZipWith[A, Option[Fact], Bind[A]]((msg: A, tag: Option[Fact]) => tag match {
            case Some(fact) =>
              Bind(Set(fact), msg)

            case None =>
              Bind(Set.empty, msg)
          })

          sensor ~> zip.left
          broadcast ~> zip.right
          zip.out ~> out(location)
        }
      }
    }
  }

  object ModulateSensorNet {
    def apply[A, B, L](locations: Set[L]) = new ModulateSensorNet[A, B, L](locations)
  }

  /**
   * Flowgraph that monitors the `inputLocations` sensor network for recognisable gestures. When gestures are detected,
   * messages on the `outputLocations` sensor network are tagged and grouped.
   *
   * @param inputLocations  locations that make up the input sensor network
   * @param outputLocations locations that make up the output sensor network
   */
  class GestureClassification[L](inputLocations: Set[L], outputLocations: Set[L]) {
    require(inputLocations.nonEmpty)
    require(outputLocations.nonEmpty)

    private val identify = inputLocations.map(loc => (loc, IdentifyGestureEvents())).toMap
    private val modulate = ModulateSensorNet[AccelerometerValue, Bind[AccelerometerValue], L](outputLocations)
    private val merge = MergeSignals[Option[Fact], Option[Fact]](inputLocations.size) { (obs: Set[Option[Fact]]) =>
      obs
        .filter {
          case Some(Gesture(nm, _)) if nm == name =>
            true

          case _ =>
            false
        }
        .fold(None) {
          case (result @ Some(_), None) =>
            result

          case (None, result @ Some(_)) =>
            result

          case (Some(Gesture(_, matchProb1)), Some(Gesture(_, matchProb2))) =>
            Some(Gesture(name, matchProb1.max(matchProb2)))

          case _ =>
            None
      }
    }

    // Tapped sensors - monitored for recognisable gestures
    val inputTap = inputLocations.map(loc => (loc, identify(loc).in)).toMap
    val outputTap = inputLocations.map(loc => (loc, identify(loc).out)).toMap

    // Modulation sensors - binds sensor data with gesture facts
    val inputModulate = outputLocations.map(loc => (loc, modulate.in(loc))).toMap
    val outputModulate = outputLocations.map(loc => (loc, modulate.out(loc))).toMap

    val graph = PartialFlowGraph { implicit builder =>
      builder.importPartialFlowGraph(merge.graph)

      // Wire in tapped sensors
      for ((loc, index) <- inputLocations.zipWithIndex) {
        builder.importPartialFlowGraph(identify(loc).graph)
        builder.connect(identify(loc).tap, Flow[Fact], merge.in(index))
      }

      // Wire in modulation
      builder.importPartialFlowGraph(modulate.graph)
      builder.connect(merge.out, Flow[Fact], modulate.transform)
    }
  }

  object GestureClassification {
    def apply[L](inputLocations: Set[L], outputLocations: Set[L]) = new GestureClassification[L](inputLocations, outputLocations)
  }

}
