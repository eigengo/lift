package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}

class GroupBySampleTest extends AkkaSpec {

  import FlowGraphImplicits._
  import GroupBySample._
  import StreamTestKit._

  val settings = MaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 1)

  implicit val materializer = FlowMaterializer(settings)

  def group(in: Source[String], out: Sink[GroupValue[String]]) = FlowGraph { implicit builder =>
    in ~> Flow[String].transform(() => GroupBySample[String](5) { sample =>
      if (sample.last == "single") {
        SingleValue(sample.mkString("-"))
      } else {
        BlobValue(sample)
      }
    }) ~> out
  }

  "GroupBySample" must {
    "receive elements, but not emit them whilst its internal buffer is not full" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[GroupValue[String]]()

      val workflow = group(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      out.expectNoMsg()
    }

    "a saturated GroupBySample should emit elements in the order they are received" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "single")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[GroupValue[String]]()

      val workflow = group(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      out.expectNext(BlobValue(msgs.slice(0, 5)))
      out.expectNoMsg() // since buffer is saturated and no more messages are arriving
    }

    "closing a partially full GroupBySample should flush buffered elements" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[GroupValue[String]]()

      val workflow = group(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      out.expectNext(BlobValue(msgs))
      out.expectComplete()
    }

    "closing a saturated GroupBySample should flush buffered elements" in {
      val msgs = List("one", "two", "three", "four", "single", "six", "seven")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[GroupValue[String]]()

      val workflow = group(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      out.expectNext(SingleValue(msgs.slice(0, 5).mkString("-")), BlobValue(msgs.slice(1, 6)), BlobValue(msgs.slice(6, 7)))
      out.expectComplete()
    }

    "exceptions (i.e. catastrophic stream errors) on a partially full GroupBySample materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[GroupValue[String]]()

      val workflow = group(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      out.expectError(exn)
    }

    "exceptions (i.e. catastrophic stream errors) on a saturated GroupBySample materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three", "four", "single", "six")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[GroupValue[String]]()

      val workflow = group(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      out.expectNext(SingleValue(msgs.slice(0, 5).mkString("-")), BlobValue(msgs.slice(1, 6)))
      out.expectError(exn)
    }
  }

}
