package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{ AkkaSpec, StreamTestKit }

class SlidingWindowTest extends AkkaSpec {

  import FlowGraphImplicits._
  import StreamTestKit._

  val settings = ActorFlowMaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 1)

  implicit val materializer = ActorFlowMaterializer(settings)

  def sample(in: Source[String], out: Sink[List[String]]) = FlowGraph { implicit builder =>
    in ~> Flow[String].transform(() => SlidingWindow[String](5)) ~> out
  }

  "SlidingWindow" must {
    "SlidingWindow should receive elements, but not emit them whilst its internal buffer is not full" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      out.expectNoMsg()
    }

    "a saturated SlidingWindow should emit elements in the order they are received" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      out.expectNext(msgs.slice(0, 5), msgs.slice(1, 6), msgs.slice(2, 7))
      out.expectNoMsg() // since buffer is saturated and no more messages are arriving
    }

    "a saturated SlidingWindow should emit elements in the order they are received [lots of input]" in {
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      for (n <- 0 to (limit - 5 + 1)) {
        out.expectNext(msgs.slice(n, n+5).toList)
      }
      out.expectNoMsg() // since buffer is saturated and no more messages are arriving
    }

    "closing a partially full SlidingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      out.expectNext(msgs.slice(0, 5), msgs.slice(1, 6), msgs.slice(2, 7))
      out.expectComplete()
    }

    "closing a saturated SlidingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      out.expectNext(msgs.slice(0, 5), msgs.slice(1, 6), msgs.slice(2, 7), msgs.slice(3, 8), msgs.slice(4, 9), msgs.slice(5, 10), msgs.slice(6, 11))
      out.expectComplete()
    }

    "closing a saturated SlidingWindow should flush buffered elements [lots of input]" in {
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      for (n <- 0 to limit) {
        out.expectNext(msgs.slice(n, n+5).toList)
      }
      out.expectComplete()
    }

    "exceptions (i.e. catastrophic stream errors) on a partially full SlidingWindow materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
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

    "exceptions (i.e. catastrophic stream errors) on a saturated SlidingWindow materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three", "four", "five", "six")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      out.expectNext(msgs.slice(0, 5), msgs.slice(1, 6))
      out.expectError(exn)
    }

    "exceptions (i.e. catastrophic stream errors) on a saturated SlidingWindow materialise 'immediately' [lots of input]" in {
      val exn = new RuntimeException("fake error")
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      val workflow = sample(Source(in), Sink(out))
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      for (n <- 0 to (limit - 5 + 1)) {
        out.expectNext(msgs.slice(n, n+5).toList)
      }
      out.expectError(exn)
      out.expectNoMsg()
    }

  }

}
