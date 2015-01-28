package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.FlattenStrategy
import akka.stream.scaladsl._
import breeze.linalg.DenseMatrix
import com.eigengo.lift.exercise.AccelerometerValue
import com.eigengo.lift.exercise.classifiers.svm.{SVMClassifier, SVMModelParser}
import com.typesafe.config.Config

object GestureTagging {

  /**
   * Trait that allows stream data to be tagged and classified. Data can be recognised as being the start of a gesture
   * (c.f. `GestureTag`) or as being part of an activity window (c.f. `ActivityTag`).
   */
  sealed trait TaggedValue[A]
  case class GestureTag[A](name: String, matchProbability: Double, value: A) extends TaggedValue[A]
  case class ActivityTag[A](value: A) extends TaggedValue[A]

}

/**
 * Trait that implements reactive stream components that can:
 *
 *   * [identifyGestureEvents] tap into sensor streams and trigger transformation events whenever a sample window is classified as a gesture
 *   * [mergeTransformations]  merge collections of transformation events into a single transformation event
 *   * [modulateSensorNet]     modulate the signals (e.g. by tagging them) in a network of sensors using a transformation signal
 *   * [gestureCollector]      split tagged sensor streams using highest probability gesture matches
 */
// TODO: calling code needs to "normalise" sensor stream against the time dimension - i.e. here we assume that events occur with a known frequency (use `TickSource` as a driver for this?)
trait GestureWorkflows extends SVMClassifier {

  import FlowGraphImplicits._
  import GestureTagging._
  import GroupBySample._

  def name: String
  def config: Config

  def frequency = {
    val value = config.getInt("classification.frequency")
    assert(value > 0)
    value
  }
  def threshold = {
    val value = config.getDouble(s"classification.gesture.$name.threshold")
    assert(0 <= value && value <= 1)
    value
  }
  def windowSize = {
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
   * Flowgraph that taps the in-out stream and, if a gesture is recognised, sends a tagged message to the `tap` sink.
   */
  class IdentifyGestureEvents {
    val in = UndefinedSource[AccelerometerValue]
    val out = UndefinedSink[AccelerometerValue]
    val tap = UndefinedSink[TaggedValue[AccelerometerValue]]

    val graph = PartialFlowGraph { implicit builder =>
      val split = Broadcast[AccelerometerValue]

      in ~> split ~> out
      split ~> Flow[AccelerometerValue].transform(() => SlidingWindow[AccelerometerValue](windowSize)).map { (sample: List[AccelerometerValue]) =>
        if (sample.length == windowSize) {
          // Saturated windows may be classified
          val matchProbability = probabilityOfGestureEvent(sample)

          if (matchProbability > threshold) {
            GestureTag[AccelerometerValue](name, matchProbability, sample.head)
          } else {
            ActivityTag[AccelerometerValue](sample.head)
          }
        } else {
          // Truncated windows are never classified (these typically occur when stream closes)
          ActivityTag[AccelerometerValue](sample.head)
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
  class MergeTagging[A, B](size: Int, merge: Set[A] => B) {
    require(size > 0)

    val in = (0 until size).map(_ => UndefinedSource[A])
    val out = UndefinedSink[B]

    val graph = PartialFlowGraph { implicit builder =>
      val zip = ZipSet[A](in.size)

      for ((probe, index) <- in.zipWithIndex) {
        probe ~> zip.in(index)
      }
      zip.out ~> Flow[Set[A]].map(merge) ~> out
    }
  }

  object MergeTagging {
    def apply[A, B](size: Int)(merge: Set[A] => B) = {
      new MergeTagging[A, B](size, merge)
    }
  }

  /**
   * Flowgraph that modulates all sensors in a network of location tagged sensors. Messages on the `transform` source
   * determine how signals in the sensor net are modulated or transformed.
   *
   * @param locations set of locations that make up the sensor network
   */
  class ModulateSensorNet[A, B, L](locations: Set[L]) {
    val in = locations.map(loc => (loc, UndefinedSource[A])).toMap
    val transform = UndefinedSource[TaggedValue[B]]
    val out = locations.map(loc => (loc, UndefinedSink[TaggedValue[A]])).toMap

    val graph = PartialFlowGraph { implicit builder =>
      val broadcast = Broadcast[TaggedValue[B]]

      transform ~> broadcast
      for ((location, sensor) <- in) {
        val zip = ZipWith[A, TaggedValue[B], TaggedValue[A]]((msg: A, tag: TaggedValue[B]) => tag match {
          case GestureTag(name, matchProb, _) =>
            GestureTag(name, matchProb, msg)

          case ActivityTag(_) =>
            ActivityTag(msg)
        })

        sensor    ~> zip.left
        broadcast ~> zip.right
        zip.out   ~> out(location)
      }
    }
  }

  object ModulateSensorNet {
    def apply[A, B, L](locations: Set[L]) = new ModulateSensorNet[A, B, L](locations)
  }

  /**
   * Flowgraph that groups messages on the input source.
   */
  class GestureGrouping[A] {
    val in = UndefinedSource[TaggedValue[A]]
    val out = UndefinedSink[GroupValue[TaggedValue[A]]]
/*
    val graph = PartialFlowGraph { implicit builder =>
      in ~>
        Flow[TaggedValue[A]]
          .splitWhen(_.isInstanceOf[ActivityTag[A]])
          .map { (events: Source[TaggedValue[A]]) =>
            events.scan((Seq.empty[GroupValue[TaggedValue[A]]], List.empty[GestureTag[A]])) {
              case ((aggr: Seq[GroupValue[TaggedValue[A]]], last: List[GestureTag[A]]), evnt: TaggedValue[A]) =>
                if (last.isEmpty && evnt.isInstanceOf[ActivityTag[A]]) {
                  (aggr :+ SingleValue(evnt), List.empty)
                } else if (last.isEmpty && evnt.isInstanceOf[GestureTag[A]]) {
                  (aggr, List(evnt.asInstanceOf[GestureTag[A]]))
                } else if (evnt.isInstanceOf[ActivityTag[A]]) {
                  (aggr :+ BlobValue[TaggedValue[A]](last) :+ SingleValue(evnt), List.empty)
                } else if (last.head.name != evnt.asInstanceOf[GestureTag[A]].name) {
                  (aggr :+ BlobValue[TaggedValue[A]](last), List(evnt.asInstanceOf[GestureTag[A]]))
                } else {
                  (aggr, last :+ evnt.asInstanceOf[GestureTag[A]])
                }
            }
          } ~> out
    }
*/
    val graph = PartialFlowGraph { implicit builder =>
      in ~> Flow[TaggedValue[A]].transform(() => GroupBySample[TaggedValue[A]](2 * windowSize) { (sample: List[TaggedValue[A]]) =>
        require(sample.length == 2 * windowSize)

        val gestures = sample.take(windowSize).takeWhile {
          case elem: GestureTag[A] =>
            true

          case _ =>
            false
        }.map(_.asInstanceOf[GestureTag[A]])

        if (gestures.isEmpty) {
          // Sample prefix contains no recognisable gestures
          SingleValue(sample.head)
        } else {
          // Sample prefix contains at least one recognisable gesture - we need to check if the head message has highest recognition probability
          if (gestures.head.matchProbability >= gestures.map(_.matchProbability).max) {
            // Head of sample window is our best match - emit it
            BlobValue(gestures.slice(0, windowSize))
          } else {
            // Sample window is not yet at the best match
            SingleValue(gestures.head)
          }
        }
      }) ~> out
    }
  }

  object GestureGrouping {
    def apply[A]() = new GestureGrouping[A]()
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
    private val group = inputLocations.map(loc => (loc, GestureGrouping[AccelerometerValue]())).toMap

    // Tapped sensors - monitored for recognisable gestures
    val inputTap = inputLocations.map(loc => (loc, identify(loc).in)).toMap
    val outputTap = inputLocations.map(loc => (loc, identify(loc).out)).toMap
    // Grouped sensors - event streams are tagged and grouped
    val groupedIn = outputLocations.map(loc => (loc, group(loc).in)).toMap
    val groupedOutput = outputLocations.map(loc => (loc, group(loc).out)).toMap

    val graph = PartialFlowGraph { implicit builder =>
      val modulate = ModulateSensorNet[AccelerometerValue, TaggedValue[AccelerometerValue], L](outputLocations)
      val merge = MergeTagging[TaggedValue[AccelerometerValue], TaggedValue[AccelerometerValue]](inputLocations.size) { (obs: Set[TaggedValue[AccelerometerValue]]) =>
        require(obs.nonEmpty)

        if (obs.filter(_.isInstanceOf[GestureTag[AccelerometerValue]]).nonEmpty) {
          obs.filter(_.isInstanceOf[GestureTag[AccelerometerValue]]).asInstanceOf[Set[GestureTag[AccelerometerValue]]].maxBy(_.matchProbability)
        } else {
          obs.head
        }
      }

      // Wire in tapped sensors
      for ((loc, index) <- inputLocations.zipWithIndex) {
        builder.importPartialFlowGraph(identify(loc).graph)
        builder.connect(identify(loc).tap, Flow[TaggedValue[AccelerometerValue]], merge.in(index))
      }

      // Wire in modulation
      builder.importPartialFlowGraph(modulate.graph)
      builder.connect(merge.out, Flow[TaggedValue[AccelerometerValue]], modulate.transform)

      // Wire in grouping
      for (loc <- outputLocations) {
        builder.importPartialFlowGraph(group(loc).graph)
        builder.connect(modulate.out(loc), Flow[TaggedValue[AccelerometerValue]], group(loc).in)
      }
    }
  }

  object GestureClassification {
    def apply[L](inputLocations: Set[L], outputLocations: Set[L]) = new GestureClassification[L](inputLocations, outputLocations)
  }

}
