package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}

class ZipNodesTest extends AkkaSpec {

  import FlowGraphImplicits._
  import StreamTestKit._

  val settings = ActorFlowMaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 4)

  implicit val materializer = ActorFlowMaterializer(settings)

  "ZipN(3)" must {

    def merge(in: Set[Source[String]], out: Sink[Set[String]]) = FlowGraph { implicit builder =>
      val zip = ZipN[String](in.size)

      for ((probe, index) <- in.zipWithIndex) {
        probe ~> zip.in(index)
      }
      zip.out ~> out
    }

    "not output anything if it receives nothing" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)

      out.expectNoMsg()
    }

    "not output anything if only one of its inputs receives a message" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNoMsg()
    }

    "not output anything if only two of its inputs receive messages" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNoMsg()
    }

    "output if all three inputs receive messages" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two", "three")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext(Set("one", "two", "three"))
    }

    "output multiple messages in the order they were received" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(3)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext(Set("one", "two", "three"), Set("four", "five", "six"))
    }

    "closing an upstream should close the entire flow" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two", "three", "four", "five")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(2)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      inPub(2).sendComplete()

      out.expectNext(Set("one", "two", "three"))
      out.expectComplete()
    }

    "an upstream error should error the entire flow" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three", "four", "five")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(2)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      inPub(0).sendError(exn)

      out.expectNext(Set("one", "two", "three"))
      out.expectError(exn)
    }
  }

  "ZipSet(Set(1, 2, 3))" must {

    def merge(in: Set[Source[String]], out: Sink[Set[String]]) = FlowGraph { implicit builder =>
      val zip = ZipSet[String, Int]((0 until in.size).toSet)

      for ((probe, index) <- in.zipWithIndex) {
        probe ~> zip.in(index)
      }
      zip.out ~> out
    }

    "not output anything if it receives nothing" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)

      out.expectNoMsg()
    }

    "not output anything if only one of its inputs receives a message" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNoMsg()
    }

    "not output anything if only two of its inputs receive messages" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNoMsg()
    }

    "output if all three inputs receive messages" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two", "three")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(1)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext(Set("one", "two", "three"))
    }

    "output multiple messages in the order they were received" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two", "three", "four", "five", "six", "seven")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(3)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }

      out.expectNext(Set("one", "two", "three"), Set("four", "five", "six"))
    }

    "closing an upstream should close the entire flow" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val msgs = List("one", "two", "three", "four", "five")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(2)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      inPub(2).sendComplete()

      out.expectNext(Set("one", "two", "three"))
      out.expectComplete()
    }

    "an upstream error should error the entire flow" in {
      val inProbe = (0 until 3).map(_ => PublisherProbe[String]())
      val out = SubscriberProbe[Set[String]]()
      val exn = new RuntimeException("fake error")
      val msgs = List("one", "two", "three", "four", "five")

      val workflow = merge(inProbe.map(Source.apply[String]).toSet, Sink(out))
      workflow.run()
      val inPub = inProbe.map(in => new AutoPublisher(in))
      val sub = out.expectSubscription()

      sub.request(2)
      for ((msg, n) <- msgs.zipWithIndex) {
        inPub(n % 3).sendNext(msg)
      }
      inPub(0).sendError(exn)

      out.expectNext(Set("one", "two", "three"))
      out.expectError(exn)
    }
  }

}
