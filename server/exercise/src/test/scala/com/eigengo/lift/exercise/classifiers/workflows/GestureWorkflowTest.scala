package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}
import com.eigengo.lift.exercise.AccelerometerValue
import com.typesafe.config.ConfigFactory
import scala.io.{Source => IOSource}

class GestureWorkflowTest extends AkkaSpec(ConfigFactory.load("classification.conf")) with GestureWorkflows {

  import FlowGraphImplicits._
  import GestureTagging._
  import GroupBySample._
  import StreamTestKit._

  val name = "tap"
  val config = system.settings.config

  val settings = MaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 1024)

  implicit val materializer = FlowMaterializer(settings)

  val accelerometerData = Option(getClass.getResource("/samples/tap.csv")).map { dataFile =>
    IOSource.fromURL(dataFile, "UTF-8").getLines().map(line => { val List(x, y, z) = line.split(",").toList.map(_.toInt); AccelerometerValue(x, y, z) })
  }.get.toList
  val noTapEvents = accelerometerData.slice(600, accelerometerData.length)
  val tapEvents = accelerometerData.slice(0, 600)

  "IdentifyGestureEvents" must {

    def component(in: PublisherProbe[AccelerometerValue], out: SubscriberProbe[AccelerometerValue], tap: SubscriberProbe[TaggedValue[AccelerometerValue]]) = FlowGraph { implicit builder =>
      val identify = IdentifyGestureEvents()

      builder.importPartialFlowGraph(identify.graph)

      builder.attachSource(identify.in, Source(in))
      builder.attachSink(identify.out, Sink(out))
      builder.attachSink(identify.tap, Sink(tap))
    }

    "in messages should pass through unaltered and tap's are not detected [no tap request]" in {
      val msgs = noTapEvents
      val inProbe = PublisherProbe[AccelerometerValue]()
      val outProbe = SubscriberProbe[AccelerometerValue]()
      val tapProbe = SubscriberProbe[TaggedValue[AccelerometerValue]]()

      component(inProbe, outProbe, tapProbe).run()
      val inPub = inProbe.expectSubscription()
      val outSub = outProbe.expectSubscription()
      val tapSub = tapProbe.expectSubscription()

      outSub.request(msgs.length)
      tapSub.request(msgs.length)
      for (msg <- msgs) {
        inPub.sendNext(msg)
      }
      inPub.sendComplete()

      outProbe.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      for ((msg, index) <- msgs.zipWithIndex) {
        tapProbe.expectNext(ActivityTag(msg))
      }
    }

    "in messages should pass through unaltered and tap is detected [tap request]" in {
      val msgs = tapEvents
      val gestureWindow = List(256 until 290, 341 until 344, 379 until 408, 546 until 576).flatten.toList
      val inProbe = PublisherProbe[AccelerometerValue]()
      val outProbe = SubscriberProbe[AccelerometerValue]()
      val tapProbe = SubscriberProbe[TaggedValue[AccelerometerValue]]()

      component(inProbe, outProbe, tapProbe).run()
      val inPub = inProbe.expectSubscription()
      val outSub = outProbe.expectSubscription()
      val tapSub = tapProbe.expectSubscription()

      outSub.request(msgs.length)
      tapSub.request(msgs.length)
      for (msg <- msgs) {
        inPub.sendNext(msg)
      }
      inPub.sendComplete()

      outProbe.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      for ((msg, index) <- msgs.zipWithIndex) {
        val event = tapProbe.expectNext()
        if (gestureWindow.contains(index)) {
          event shouldBe a[GestureTag[_]]
          event.asInstanceOf[GestureTag[AccelerometerValue]].name should be("tap")
          event.asInstanceOf[GestureTag[AccelerometerValue]].matchProbability should be > 0.75
          event.asInstanceOf[GestureTag[AccelerometerValue]].value should be(msg)
        } else {
          event should be(ActivityTag(msg))
        }
      }
    }

  }

  "MergeTagging" must {

    val merge = MergeTagging[TaggedValue[String], TaggedValue[String]](3) { (obs: Set[TaggedValue[String]]) =>
      require(obs.nonEmpty)

      if (obs.filter(_.isInstanceOf[GestureTag[String]]).nonEmpty) {
        obs.filter(_.isInstanceOf[GestureTag[String]]).asInstanceOf[Set[GestureTag[String]]].maxBy(_.matchProbability)
      } else {
        obs.head
      }
    }
    def component(inProbe: List[PublisherProbe[TaggedValue[String]]], out: SubscriberProbe[TaggedValue[String]]) = FlowGraph { implicit builder =>
      builder.importPartialFlowGraph(merge.graph)

      for (n <- 0 until 3) {
        builder.attachSource(merge.in(n), Source(inProbe(n)))
      }
      builder.attachSink(merge.out, Sink(out))
    }

    "correctly merge tagged accelerometer data values by selecting gesture with largest matching probability" in {
      val msgs: List[TaggedValue[String]] = List(GestureTag("one", 0.75, "1"), ActivityTag("2"), GestureTag("three", 0.80, "3"))
      val inProbe = (0 until 3).map(_ => PublisherProbe[TaggedValue[String]]()).toList
      val out = SubscriberProbe[TaggedValue[String]]()

      component(inProbe, out).run()
      val inPub = inProbe.map(_.expectSubscription())
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext(msgs(2))
    }

    "correctly merge tagged accelerometer data values when no signals are a gesture match" in {
      val msgs: List[TaggedValue[String]] = List(ActivityTag("1"), ActivityTag("2"), ActivityTag("3"))
      val inProbe = (0 until 3).map(_ => PublisherProbe[TaggedValue[String]]()).toList
      val out = SubscriberProbe[TaggedValue[String]]()

      component(inProbe, out).run()
      val inPub = inProbe.map(_.expectSubscription())
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext(msgs(0))
    }

  }

  "ModulateSensorNet" must {

    val modulate = ModulateSensorNet[String, TaggedValue[String], Int]((0 until 3).toSet)

    def component(in: Map[Int, PublisherProbe[String]], transform: PublisherProbe[TaggedValue[String]], out: Map[Int, SubscriberProbe[TaggedValue[String]]]) = FlowGraph { implicit builder =>
      builder.importPartialFlowGraph(modulate.graph)

      for (loc <- 0 until 3) {
        builder.attachSource(modulate.in(loc), Source(in(loc)))
      }
      builder.attachSource(modulate.transform, Source(transform))
      for (loc <- 0 until 3) {
        builder.attachSink(modulate.out(loc), Sink(out(loc)))
      }
    }

    "request for output on at least one wire (input values present) should be correctly transformed" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[TaggedValue[String]]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      outSub(1).request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(ActivityTag("default"))

      outProbe(1).expectNext(ActivityTag("two"))
    }

    "request for outputs on all 3 wires (input values present) should be correctly transformed [no gesture]" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[TaggedValue[String]]()

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
      transformPub.sendNext(ActivityTag("default"))

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
      }
    }

    "request for outputs on all 3 wires (input values present) should be correctly transformed [gesture present]" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[TaggedValue[String]]()

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
      transformPub.sendNext(GestureTag("transform", 0.42, "default"))

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(GestureTag("transform", 0.42, msgs(n)))
      }
    }

    "multiple requests for output on all 3 wires (input values present) should be correctly transformed [no gesture]" in {
      val msgs = List("one", "two", "three", "four", "five", "six")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[TaggedValue[String]]()

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
      transformPub.sendNext(ActivityTag("default-1"))
      transformPub.sendNext(ActivityTag("default-2"))

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
      }
    }

    "multiple requests for output on all 3 wires (input values present) should be correctly transformed [gesture present]" in {
      val msgs = List("one", "two", "three", "four", "five", "six")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[TaggedValue[String]]())).toMap
      val transformProbe = PublisherProbe[TaggedValue[String]]()

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
      transformPub.sendNext(GestureTag("transform", 0.42, "default-1"))
      transformPub.sendNext(ActivityTag("default-2"))

      for (n <- 0 until msgs.length) {
        if (n / 3 == 0) {
          outProbe(n % 3).expectNext(GestureTag("transform", 0.42, msgs(n)))
        } else {
          outProbe(n % 3).expectNext(ActivityTag(msgs(n)))
        }
      }
    }

 }

  "GestureGrouping" must {

    def component(in: PublisherProbe[TaggedValue[String]], out: SubscriberProbe[GroupValue[TaggedValue[String]]]) = FlowGraph { implicit builder =>
      val group = GestureGrouping[String]()

      builder.importPartialFlowGraph(group.graph)

      builder.attachSource(group.in, Source(in))
      builder.attachSink(group.out, Sink(out))
    }

    "group single gesture tagged value with highest windowed match" in {
      val msgs = ((0 until 10).map(n => ActivityTag[String](s"activity-$n")) ++ (2 to 10).map(n => GestureTag[String](name, 0.90 - (1.0 / n), s"gesture-$n")) ++ (10 until 20).map(n => ActivityTag[String](s"activity-$n"))).toList
      val inProbe = PublisherProbe[TaggedValue[String]]()
      val outProbe = SubscriberProbe[GroupValue[TaggedValue[String]]]()

      component(inProbe, outProbe).run()
      val inPub = inProbe.expectSubscription()
      val outSub = outProbe.expectSubscription()

      outSub.request(msgs.length)
      for (msg <- msgs) {
        inPub.sendNext(msg)
      }
      inPub.sendComplete()

      for (msg <- msgs.slice(0, 10).map(SingleValue[TaggedValue[String]](_))) {
        outProbe.expectNext(msg)
      }
      for (msg <- msgs.slice(10, 10+8).map(SingleValue[TaggedValue[String]](_))) {
        outProbe.expectNext(msg)
      }
      outProbe.expectNext(BlobValue(List(msgs(10+9))))
      for (msg <- msgs.slice(10+9, 10+9+10).map(SingleValue[TaggedValue[String]](_))) {
        outProbe.expectNext(msg)
      }
    }

    "group contiguous gesture tagged values with highest windowed match being recorded" in {
      val msgs = ((0 until 10).map(n => ActivityTag[String](s"activity-$n")) ++ (2 to 10).map(n => GestureTag[String](name, 0.30 + (1.0 / n), s"gesture-$n")) ++ (10 until 20).map(n => ActivityTag[String](s"activity-$n"))).toList
      val inProbe = PublisherProbe[TaggedValue[String]]()
      val outProbe = SubscriberProbe[GroupValue[TaggedValue[String]]]()

      component(inProbe, outProbe).run()
      val inPub = inProbe.expectSubscription()
      val outSub = outProbe.expectSubscription()

      outSub.request(msgs.length)
      for (msg <- msgs) {
        inPub.sendNext(msg)
      }
      inPub.sendComplete()

      for (msg <- msgs.slice(0, 10).map(SingleValue[TaggedValue[String]](_))) {
        outProbe.expectNext(msg)
      }
      outProbe.expectNext(BlobValue(msgs.slice(10, 10+9)))
      for (msg <- msgs.slice(10+9, 10+9+10).map(SingleValue[TaggedValue[String]](_))) {
        outProbe.expectNext(msg)
      }
    }

  }

  "GestureClassification" must {

    "" in {
      // TODO:
    }

  }

}
