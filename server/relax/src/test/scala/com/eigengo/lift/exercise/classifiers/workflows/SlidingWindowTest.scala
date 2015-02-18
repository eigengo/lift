package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{ AkkaSpec, StreamTestKit }

class SlidingWindowTest extends AkkaSpec {

  import StreamTestKit._

  val settings = ActorFlowMaterializerSettings(system)//.withInputBuffer(initialSize = 1, maxSize = 1)

  implicit val materializer = ActorFlowMaterializer(settings)

  val windowSize = 10
  assert(windowSize > 1)

  def sample(in: Source[String], out: Sink[List[String]]) =
    Flow[String].transform(() => SlidingWindow[String](windowSize)).runWith(in, out)

  "SlidingWindow" must {
    "SlidingWindow should receive elements, but not emit them whilst its internal buffer is not full" in {
      val msgs = (0 until (windowSize-1)).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      out.expectNoMsg()
    }

    "a saturated SlidingWindow should emit elements in the order they are received" in {
      val msgs = (0 until (windowSize+2)).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      out.expectNext(msgs.slice(0, windowSize), msgs.slice(1, windowSize+1), msgs.slice(2, windowSize+2))
      out.expectNoMsg() // since buffer is saturated and no more messages are arriving
    }

    "a saturated SlidingWindow should emit elements in the order they are received [lots of input; publisher source]" in {
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      for (n <- 0 to (limit - windowSize + 1)) {
        out.expectNext(msgs.slice(n, n+windowSize))
      }
      out.expectNoMsg() // since buffer is saturated and no more messages are arriving
    }

    "a saturated SlidingWindow should emit elements in the order they are received [lots of input; iterable source]" in {
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n").toList
      val out = SubscriberProbe[List[String]]()

      sample(Source(msgs), Sink(out))
      val sub = out.expectSubscription()
      sub.request(msgs.length)

      for (n <- 0 to (limit - windowSize + 1)) {
        out.expectNext(msgs.slice(n, n+windowSize))
      }
      // since iterable source closes, and no more messages are arriving, contents will flush out
      for (n <- (limit - windowSize + 1 + 1) to limit) {
        out.expectNext(msgs.slice(n, n+windowSize))
      }
    }

    "closing a partially full SlidingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      out.expectNext(msgs.slice(0, windowSize), msgs.slice(1, windowSize+1), msgs.slice(2, windowSize+2))
      out.expectComplete()
    }

    "closing a saturated SlidingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      out.expectNext(msgs.slice(0, windowSize), msgs.slice(1, windowSize+1), msgs.slice(2, windowSize+2), msgs.slice(3, windowSize+3), msgs.slice(4, windowSize+4), msgs.slice(5, windowSize+5), msgs.slice(6, windowSize+6))
      out.expectComplete()
    }

    "closing a saturated SlidingWindow should flush buffered elements [lots of input]" in {
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      for (n <- 0 to limit) {
        out.expectNext(msgs.slice(n, n+windowSize))
      }
      out.expectComplete()
    }

    "exceptions (i.e. catastrophic stream errors) on a partially full SlidingWindow materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = (0 until (windowSize-1)).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
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
      val msgs = (0 until (windowSize+1)).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      out.expectNext(msgs.slice(0, windowSize), msgs.slice(1, windowSize+1))
      out.expectError(exn)
    }

    "exceptions (i.e. catastrophic stream errors) on a saturated SlidingWindow materialise 'immediately' [lots of input]" in {
      val exn = new RuntimeException("fake error")
      val limit = 1000
      val msgs = (0 to limit).map(n => s"message-$n").toList
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[List[String]]()

      sample(Source(in), Sink(out))
      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      for (n <- 0 to (limit - windowSize + 1)) {
        out.expectNext(msgs.slice(n, n+windowSize))
      }
      out.expectError(exn)
      out.expectNoMsg()
    }

  }

}
