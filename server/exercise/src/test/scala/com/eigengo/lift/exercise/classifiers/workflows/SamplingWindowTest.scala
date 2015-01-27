package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{ AkkaSpec, StreamTestKit }
import akka.testkit.TestProbe

class SamplingWindowTest extends AkkaSpec {

  import FlowGraphImplicits._
  import StreamTestKit._

  val settings = MaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 1)

  implicit val materializer = FlowMaterializer(settings)

  val senderProbe = TestProbe()

  def sample(in: Source[String], out: Sink[String])(probe: TestProbe) = FlowGraph { implicit builder =>
    in ~> Flow[String].transform(() => SamplingWindow[String](5) { sample =>
      probe.ref.tell(sample, senderProbe.ref)
    }) ~> out
  }

  "SamplingWindow" must {
    "SamplingWindow should receive elements, but not emit them whilst its internal buffer is not full" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(Source(in), Sink(out))(probe)
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      probe.expectNoMsg()
      out.expectNoMsg()
      senderProbe.expectNoMsg()
    }

    "a saturated SamplingWindow should emit elements in the order they are received" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(Source(in), Sink(out))(probe)
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      probe.receiveN(3) should be(List(msgs.slice(0, 5), msgs.slice(1, 6), msgs.slice(2, 7)))
      out.expectNext(msgs(0), msgs(1))
      out.expectNoMsg() // since buffer is saturated and no more messages are arriving
      senderProbe.expectNoMsg()
    }

    "closing a partially full SamplingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(Source(in), Sink(out))(probe)
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      probe.expectNoMsg()
      out.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      out.expectComplete()
      senderProbe.expectNoMsg()
    }

    "closing a saturated SamplingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      // Simulate source that outputs messages and then completes
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(Source(in), Sink(out))(probe)
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendComplete()

      probe.receiveN(3) should be(List(msgs.slice(0, 5), msgs.slice(1, 6), msgs.slice(2, 7)))
      out.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      out.expectComplete()
      senderProbe.expectNoMsg()
    }

    "exceptions (i.e. catastrophic stream errors) on a partially full SamplingWindow materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(Source(in), Sink(out))(probe)
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      probe.expectNoMsg()
      out.expectError(exn)
      senderProbe.expectNoMsg()
    }

    "exceptions (i.e. catastrophic stream errors) on a saturated SamplingWindow materialise 'immediately'" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three", "four", "five", "six")
      // Simulate source that outputs messages and then errors
      val in = PublisherProbe[String]()
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(Source(in), Sink(out))(probe)
      workflow.run()
      val pub = in.expectSubscription()
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }
      pub.sendError(exn)

      probe.receiveN(2) should be(List(msgs.slice(0, 5), msgs.slice(1, 6)))
      out.expectNext(msgs(0))
      out.expectError(exn)
      senderProbe.expectNoMsg()
    }
  }

}
