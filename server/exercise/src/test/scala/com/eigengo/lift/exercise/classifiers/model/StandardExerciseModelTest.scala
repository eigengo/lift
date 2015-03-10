package com.eigengo.lift.exercise.classifiers.model

import akka.stream.{ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.scaladsl._
import akka.stream.testkit.{StreamTestKit, AkkaSpec}
import akka.testkit.TestActorRef
import com.eigengo.lift.exercise.UserExercisesClassifier.{Tap => TapEvent}
import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions.{NegGesture, Gesture, BindToSensors}
import com.eigengo.lift.exercise.{SensorDataSourceLocationWrist, AccelerometerValue, SensorNetValue, SessionProperties}
import com.typesafe.config.ConfigFactory
import java.text.SimpleDateFormat
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => IOSource}

class StandardExerciseModelTest extends AkkaSpec(ConfigFactory.load("classification.conf")) {

  import ExerciseModel._
  import StreamTestKit._

  // FIXME: why do we need a maximum prefetch buffer size of 64 here?
  val settings = ActorFlowMaterializerSettings(system).withInputBuffer(initialSize = 1, maxSize = 64)

  implicit val materializer = ActorFlowMaterializer(settings)

  val name = "tap"
  val windowSize = system.settings.config.getInt(s"classification.gesture.$name.size")
  val threshold = system.settings.config.getDouble(s"classification.gesture.$name.threshold")
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val startDate = dateFormat.parse("1970-01-01")
  val sessionProps = SessionProperties(startDate, Seq("Legs"), 1.0)
  val accelerometerData = Option(getClass.getResource(s"/samples/$name.csv")).map { dataFile =>
    IOSource.fromURL(dataFile, "UTF-8").getLines().map(line => { val List(x, y, z) = line.split(",").toList.map(_.toInt); AccelerometerValue(x, y, z) })
  }.get.toList
  val dummyValue = AccelerometerValue(0, 0, 0)
  implicit val prover = new SMTInterface {
    def simplify(query: Query)(implicit ec: ExecutionContext) = Future(query)
    def satisfiable(query: Query)(implicit ec: ExecutionContext) = Future(true)
    def valid(query: Query)(implicit ec: ExecutionContext) = Future(true)
  }

  "StandardExerciseModel workflow" must {

    def component(in: Source[SensorNetValue], out: Sink[BindToSensors]) = {
      val workflow = TestActorRef(new StandardExerciseModel(sessionProps, SensorDataSourceLocationWrist) {
        def makeDecision(query: Query, value: QueryValue, result: Boolean) = TapEvent
      }).underlyingActor.workflow
      workflow.runWith(in, out)
    }

    "correctly detect wrist sensor taps" in {
      // FIXME: is this correct?
      val msgs: List[SensorNetValue] = accelerometerData.map(d => SensorNetValue(Vector(d), Vector(dummyValue), Vector(dummyValue), Vector(dummyValue), Vector(dummyValue)))
      val tapIndex = List(256 until 290, 341 until 344, 379 until 408, 546 until 577).flatten.toList
      // Simulate source that outputs messages and then blocks
      val in = PublisherProbe[SensorNetValue]()
      val out = SubscriberProbe[BindToSensors]()

      component(Source(in), Sink(out))

      val pub = new AutoPublisher(in)
      val sub = out.expectSubscription()
      sub.request(msgs.length)
      for (msg <- msgs) {
        pub.sendNext(msg)
      }

      for (index <- 0 to (msgs.length - windowSize)) {
        val fact = if (tapIndex.contains(index)) Gesture(name, threshold) else NegGesture(name, threshold)

        out.expectNext(BindToSensors(Set(fact), Set(), Set(), Set(), Set(), msgs(index)))
      }
    }
  }

}
