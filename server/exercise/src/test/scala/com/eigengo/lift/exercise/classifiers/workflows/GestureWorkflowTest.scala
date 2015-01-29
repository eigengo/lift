package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}
import com.eigengo.lift.exercise.AccelerometerValue
import com.typesafe.config.ConfigFactory
import scala.io.{Source => IOSource}

class GestureWorkflowTest extends AkkaSpec(ConfigFactory.load("classification.conf")) with GestureWorkflows {

  import FlowGraphImplicits._
  import ClassificationAssertions._
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

    def component(in: PublisherProbe[AccelerometerValue], out: SubscriberProbe[AccelerometerValue], tap: SubscriberProbe[Fact]) = FlowGraph { implicit builder =>
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
      val tapProbe = SubscriberProbe[Fact]()

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
        tapProbe.expectNext(Unknown)
      }
    }

    "in messages should pass through unaltered and tap is detected [tap request]" in {
      val msgs = tapEvents
      val gestureWindow = List(256 until 290, 341 until 344, 379 until 408, 546 until 576).flatten.toList
      val inProbe = PublisherProbe[AccelerometerValue]()
      val outProbe = SubscriberProbe[AccelerometerValue]()
      val tapProbe = SubscriberProbe[Fact]()

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
          event shouldBe a[Gesture]
          event.asInstanceOf[Gesture].name should be("tap")
          event.asInstanceOf[Gesture].matchProbability should be > 0.75
        } else {
          event should be(Unknown)
        }
      }
    }

  }

  "MergeTagging" must {

    val merge = MergeTagging[Fact, Fact](3) { (obs: Set[Fact]) =>
      require(obs.nonEmpty)

      if (obs.filter(_.isInstanceOf[Gesture]).nonEmpty) {
        obs.filter(_.isInstanceOf[Gesture]).asInstanceOf[Set[Gesture]].maxBy(_.matchProbability)
      } else {
        obs.head
      }
    }

    def component(inProbe: List[PublisherProbe[Fact]], out: SubscriberProbe[Fact]) = FlowGraph { implicit builder =>
      builder.importPartialFlowGraph(merge.graph)

      for (n <- 0 until 3) {
        builder.attachSource(merge.in(n), Source(inProbe(n)))
      }
      builder.attachSink(merge.out, Sink(out))
    }

    "correctly merge tagged accelerometer data values by selecting gesture with largest matching probability" in {
      val msgs: List[Fact] = List(Gesture("one", 0.75), Unknown, Gesture("three", 0.80))
      val inProbe = (0 until 3).map(_ => PublisherProbe[Fact]()).toList
      val out = SubscriberProbe[Fact]()

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
      val msgs: List[Fact] = List(Unknown, Unknown, Unknown)
      val inProbe = (0 until 3).map(_ => PublisherProbe[Fact]()).toList
      val out = SubscriberProbe[Fact]()

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

    val modulate = ModulateSensorNet[String, Fact, Int]((0 until 3).toSet)

    def component(in: Map[Int, PublisherProbe[String]], transform: PublisherProbe[Fact], out: Map[Int, SubscriberProbe[Bind[String]]]) = FlowGraph { implicit builder =>
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
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[Bind[String]]())).toMap
      val transformProbe = PublisherProbe[Fact]()

      component(inProbe, transformProbe, outProbe).run()
      val inPub = inProbe.map { case (n, pub) => (n, pub.expectSubscription()) }.toMap
      val outSub = outProbe.map { case (n, sub) => (n, sub.expectSubscription()) }.toMap
      val transformPub = transformProbe.expectSubscription()

      outSub(1).request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      transformPub.sendNext(Unknown)

      outProbe(1).expectNext(Bind(Predicate(Unknown), "two"))
    }

    "request for outputs on all 3 wires (input values present) should be correctly transformed [no gesture]" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[Bind[String]]())).toMap
      val transformProbe = PublisherProbe[Fact]()

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
      transformPub.sendNext(Unknown)

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(Bind(Predicate(Unknown), msgs(n)))
      }
    }

    "request for outputs on all 3 wires (input values present) should be correctly transformed [gesture present]" in {
      val msgs = List("one", "two", "three")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[Bind[String]]())).toMap
      val transformProbe = PublisherProbe[Fact]()

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
      transformPub.sendNext(Gesture("transform", 0.42))

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(Bind(Predicate(Gesture("transform", 0.42)), msgs(n)))
      }
    }

    "multiple requests for output on all 3 wires (input values present) should be correctly transformed [no gesture]" in {
      val msgs = List("one", "two", "three", "four", "five", "six")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[Bind[String]]())).toMap
      val transformProbe = PublisherProbe[Fact]()

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
      transformPub.sendNext(Unknown)
      transformPub.sendNext(Unknown)

      for (n <- 0 until msgs.length) {
        outProbe(n % 3).expectNext(Bind(Predicate(Unknown), msgs(n)))
      }
    }

    "multiple requests for output on all 3 wires (input values present) should be correctly transformed [gesture present]" in {
      val msgs = List("one", "two", "three", "four", "five", "six")
      val inProbe = (0 until 3).map(n => (n, PublisherProbe[String]())).toMap
      val outProbe = (0 until 3).map(n => (n, SubscriberProbe[Bind[String]]())).toMap
      val transformProbe = PublisherProbe[Fact]()

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
      transformPub.sendNext(Gesture("transform", 0.42))
      transformPub.sendNext(Unknown)

      for (n <- 0 until msgs.length) {
        if (n / 3 == 0) {
          outProbe(n % 3).expectNext(Bind(Predicate(Gesture("transform", 0.42)), msgs(n)))
        } else {
          outProbe(n % 3).expectNext(Bind(Predicate(Unknown), msgs(n)))
        }
      }
    }

 }

  "GestureClassification" must {

    "" in {
      // TODO:
    }

  }

}
