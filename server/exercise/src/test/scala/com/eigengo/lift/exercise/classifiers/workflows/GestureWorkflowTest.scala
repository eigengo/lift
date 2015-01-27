package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}
import com.typesafe.config.ConfigFactory

class GestureWorkflowTest extends AkkaSpec(ConfigFactory.load("classification.conf")) with GestureWorkflows {

  import FlowGraphImplicits._
  import GestureTagging._
  import GroupBySample._
  import StreamTestKit._

  val name = "testing"
  val config = system.settings.config

  val settings = MaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 1)

  implicit val materializer = FlowMaterializer(settings)

  "IdentifyGestureEvents" must {
    "" in {
      // TODO:
    }
  }

  "MergeTransformations" must {

    val merge = MergeTransformations[String, TaggedValue[String]](3) { (obs: Set[Transformation[String, TaggedValue[String]]]) =>
      require(obs.nonEmpty)

      Transformation({ value =>
        val results = obs.map(_.action(value))
        if (results.filter(_.isInstanceOf[GestureTag[String]]).nonEmpty) {
          results.filter(_.isInstanceOf[GestureTag[String]]).asInstanceOf[Set[GestureTag[String]]].maxBy(_.matchProbability)
        } else {
          results.head
        }
      })
    }
    def component(inProbe: List[PublisherProbe[Transformation[String, TaggedValue[String]]]], out: SubscriberProbe[Transformation[String, TaggedValue[String]]]) = FlowGraph { implicit builder =>
      builder.importPartialFlowGraph(merge.graph)

      for (n <- 0 until 3) {
        builder.attachSource(merge.in(n), Source(inProbe(n)))
      }
      builder.attachSink(merge.out, Sink(out))
    }

    "correctly merge tagged accelerometer data values by selecting gesture with largest matching probability" in {
      val msgs: List[Transformation[String, TaggedValue[String]]] = List(Transformation(_ => GestureTag("one", 0.75, "1")), Transformation(_ => ActivityTag("2")), Transformation(_ => GestureTag("three", 0.80, "3")))
      val inProbe = (0 until 3).map(_ => PublisherProbe[Transformation[String, TaggedValue[String]]]()).toList
      val out = SubscriberProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, out).run()
      val inPub = inProbe.map(_.expectSubscription())
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext().action("any") should be(msgs(2).action("any"))
    }

    "correctly merge tagged accelerometer data values when no signals are a gesture match" in {
      val msgs: List[Transformation[String, TaggedValue[String]]] = List(Transformation(_ => ActivityTag("1")), Transformation(_ => ActivityTag("2")), Transformation(_ => ActivityTag("3")))
      val inProbe = (0 until 3).map(_ => PublisherProbe[Transformation[String, TaggedValue[String]]]()).toList
      val out = SubscriberProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, out).run()
      val inPub = inProbe.map(_.expectSubscription())
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext().action("any") should be(msgs(0).action("any"))
    }
  }

  "ModulateSensorNet" must {
    "" in {
      // TODO:
    }
  }

  "GestureGrouping" must {
    "" in {
      // TODO:
    }
  }

  "GestureClassification" must {
    "" in {
      // TODO:
    }
  }

}
