package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{ AkkaSpec, StreamTestKit }
import akka.testkit.TestProbe

class SamplingWindowTest extends AkkaSpec {

  import FlowGraphImplicits._
  import StreamTestKit._

  val settings = MaterializerSettings(system).withInputBuffer(initialSize = 2, maxSize = 16)

  implicit val materializer = FlowMaterializer(settings)

  def sample(in: Source[String], out: Sink[String])(probe: TestProbe) = FlowGraph { implicit builder =>
    in ~> Flow[String].transform(() => SamplingWindow[String](5) { sample =>
      probe.ref ! sample
    }) ~> out
  }

  "SamplingWindow" must {
    "SamplingWindow should receive elements, but not emit them whilst its internal buffer is not full" in {
      val msgs = List("one", "two", "three")
      val in = Source(msgs)
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(in, Sink(out))(probe)
      workflow.run()
      val sub = out.expectSubscription()
      sub.request(msgs.length)

      probe.expectNoMsg()
      out.expectNoMsg()
    }

    "a saturated SamplingWindow should emit elements in the order they are received" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      val in = Source(msgs)
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(in, Sink(out))(probe)
      workflow.run()
      val sub = out.expectSubscription()
      sub.request(msgs.length)

      probe.receiveN(2) === msgs.slice(0, 2)
      out.expectNext(msgs(0), msgs(1))
    }

    "closing a partially full SamplingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three")
      val in = Source(msgs)
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(in, Sink(out))(probe)
      workflow.run()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete

      probe.expectNoMsg()
      out.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      out.expectComplete()
    }

    "closing a saturated SamplingWindow should flush buffered elements" in {
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")
      val in = Source(msgs)
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(in, Sink(out))(probe)
      workflow.run()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnComplete

      probe.receiveN(2) === msgs.slice(0, 1)
      out.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      out.expectComplete()
    }

    "errors on a partially full SamplingWindow should flush buffered elements" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three")
      val in = Source[String](() => new Iterable[String] {
        override def iterator: Iterator[String] =
          (1 until msgs.length).iterator.map(i => if (i == msgs.length) throw exn else msgs(i))
      }.iterator)
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(in, Sink(out))(probe)
      workflow.run()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnError

      probe.expectNoMsg()
      out.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      out.expectError(exn)
    }

    "errors on a saturated SamplingWindow should flush buffered elements" in {
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three", "four", "five", "six")
      val in = Source[String](() => new Iterable[String] {
        override def iterator: Iterator[String] =
          (1 until (msgs.length + 1)).iterator.map(i => if (i == msgs.length) throw exn else if (i > msgs.length) "seven" else msgs(i))
      }.iterator)
      val out = SubscriberProbe[String]()
      val probe = TestProbe()

      val workflow = sample(in, Sink(out))(probe)
      workflow.run()
      val sub = out.expectSubscription()
      sub.request(msgs.length + 1) // + OnError

      probe.receiveN(1) === msgs.head
      out.expectNext(msgs(0), msgs(1), msgs.drop(2): _*)
      out.expectError(exn)
    }
  }

}
