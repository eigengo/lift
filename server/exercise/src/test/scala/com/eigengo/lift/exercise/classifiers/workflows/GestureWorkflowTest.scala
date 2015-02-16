package com.eigengo.lift.exercise.classifiers.workflows

import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}
import com.eigengo.lift.exercise.AccelerometerValue
import com.typesafe.config.ConfigFactory
import scala.io.{Source => IOSource}

class GestureWorkflowTest extends AkkaSpec(ConfigFactory.load("classification.conf")) {

  import ClassificationAssertions._
  import StreamTestKit._

  val name = "tap"

  object Tap extends GestureWorkflows(name, system.settings.config)

  val settings = ActorFlowMaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 1)

  implicit val materializer = ActorFlowMaterializer(settings)

  val accelerometerData = Option(getClass.getResource("/samples/tap.csv")).map { dataFile =>
    IOSource.fromURL(dataFile, "UTF-8").getLines().map(line => { val List(x, y, z) = line.split(",").toList.map(_.toInt); AccelerometerValue(x, y, z) })
  }.get.toList
  val noTapEvents = accelerometerData.slice(600, accelerometerData.length)
  val tapEvents = accelerometerData.slice(0, 600)

  "IdentifyGestureEvents" must {

    def component(in: PublisherProbe[AccelerometerValue], out: SubscriberProbe[Option[Fact]]) =
      Tap.identifyEvent.runWith(Source(in), Sink(out))

    "in messages should pass through unaltered and tap's are not detected [no tap request]" in {
      val msgs = noTapEvents
      val inProbe = PublisherProbe[AccelerometerValue]()
      val outProbe = SubscriberProbe[Option[Fact]]()

      component(inProbe, outProbe)

      val inPub = new AutoPublisher(inProbe)
      val outSub = outProbe.expectSubscription()

      outSub.request(msgs.length)
      for (msg <- msgs) {
        inPub.sendNext(msg)
      }
      inPub.sendComplete()

      for ((msg, index) <- msgs.zipWithIndex) {
        if (index <= msgs.length - Tap.windowSize) {
          outProbe.expectNext(Some(NegGesture(name, Tap.threshold)))
        } else {
          outProbe.expectNext(None)
        }
      }
    }

    "in messages should pass through unaltered and tap is detected [tap request]" in {
      val msgs = tapEvents
      val gestureWindow = List(256 until 290, 341 until 344, 379 until 408, 546 until 576).flatten.toList
      val inProbe = PublisherProbe[AccelerometerValue]()
      val outProbe = SubscriberProbe[Option[Fact]]()

      component(inProbe, outProbe)

      val inPub = new AutoPublisher(inProbe)
      val outSub = outProbe.expectSubscription()

      outSub.request(msgs.length)
      for (msg <- msgs) {
        inPub.sendNext(msg)
      }
      inPub.sendComplete()

      for ((msg, index) <- msgs.zipWithIndex) {
        val event = outProbe.expectNext()
        if (index > msgs.length - Tap.windowSize) {
          event should be(None)
        } else if (gestureWindow.contains(index)) {
          event should be(Some(Gesture(name, Tap.threshold)))
        } else {
          event should be(Some(NegGesture(name, Tap.threshold)))
        }
      }
    }

  }

}
