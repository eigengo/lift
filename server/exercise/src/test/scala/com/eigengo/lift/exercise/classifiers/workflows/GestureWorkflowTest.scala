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

  val settings = MaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 4)

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

    val modulate = ModulateSensorNet[String, TaggedValue[String], Int]((0 until 3).toSet)

    def component(in: Map[Int, PublisherProbe[String]], transform: PublisherProbe[Transformation[String, TaggedValue[String]]], out: Map[Int, SubscriberProbe[TaggedValue[String]]]) = FlowGraph { implicit builder =>
      builder.importPartialFlowGraph(modulate.graph)

      for (loc <- 0 until 3) {
        builder.attachSource(modulate.in(loc), Source(in(loc)))
      }
      builder.attachSource(modulate.transform, Source(transform))
      for (loc <- 0 until 3) {
        builder.attachSink(modulate.out(loc), Sink(out(loc)))
      }
    }

    val transform = Transformation[String, TaggedValue[String]](v => if (v == "gesture") GestureTag("transform", 0.42, v) else ActivityTag(v))

    "request for output on at least one wire (input values present) should be correctly transformed" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      outSub(1).request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(transform)

      outProbe(1).expectNext(ActivityTag("two"))
    }

    "request for outputs on all 3 wires (input values present) should be correctly transformed [no gesture]" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      for (n <- 0 until 3) {
        outSub(n).request(1)
      }
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(transform)

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
      }
    }

    "request for outputs on all 3 wires (input values present) should be correctly transformed [gesture present]" in {
      val msgs = List("one", "two", "gesture")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      for (n <- 0 until 3) {
        outSub(n).request(1)
      }
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(transform)

      for (n <- 0 until msgs.length) {
        if (msgs.zipWithIndex.filter(_._1 == "gesture").map(_._2).contains(n)) {
          outProbe(n % 3).expectNext(GestureTag("transform", 0.42, msgs(n)))
        } else {
          outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
        }
      }
    }

    "multiple requests for output on all 3 wires (input values present) should be correctly transformed [no gesture]" in {
      val msgs = List("one", "two", "three", "four", "five", "six")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      for (n <- 0 until 3) {
        outSub(n).request(2)
      }
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(transform)
      transformPub.sendNext(transform)

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
      }
    }

    "multiple requests for output on all 3 wires (input values present) should be correctly transformed [gesture present]" in {
      val msgs = List("gesture", "two", "three", "four", "gesture", "six")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[Transformation[String, TaggedValue[String]]]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      for (n <- 0 until 3) {
        outSub(n).request(2)
      }
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(transform)
      transformPub.sendNext(transform)

      for (n <- 0 until msgs.length) {
        if (msgs.zipWithIndex.filter(_._1 == "gesture").map(_._2).contains(n)) {
          outProbe(n % 3).expectNext(GestureTag("transform", 0.42, msgs(n)))
        } else {
          outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
        }
      }
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
