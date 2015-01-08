package com.eigengo.lift.exercise

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActor, TestKitBase, TestProbe}
import com.eigengo.lift.common.UserId
import com.eigengo.lift.exercise.ExerciseClassifiers.{GetMuscleGroups, MuscleGroup}
import com.eigengo.lift.exercise.UserExercises._
import com.eigengo.lift.exercise.UserExercisesView._
import org.scalatest.{FlatSpec, Matchers}
import scodec.bits.BitVector
import spray.testkit.ScalatestRouteTest

import scalaz._

object ExerciseServiceTest {

  object TestData {
    val userId = UserId(UUID.randomUUID())
    val sessionId = SessionId(UUID.randomUUID())

    val squat = "squat"
    val intensity = Some(1.0)
    val startDate = new Date(1970, 1, 1, 0, 0, 0)
    val endDate = new Date(1970, 1, 1, 0, 0, 0)
    val sessionProps = SessionProps(startDate, Seq("Legs"), 1.0)
    val muscleGroups = List(MuscleGroup("legs", "Legs", List(squat, "extension", "curl")))
    val sessionSummary = List(SessionSummary(sessionId, sessionProps, Array(1.0)))
    val session = Some(ExerciseSession(sessionId, sessionProps, List(ExerciseSet(List(Exercise(squat, intensity))))))
    val sessionDates = List(SessionDate(startDate, List(SessionIntensity(intensity.get, intensity.get))))
    val bitVector = BitVector.empty
    val emptyResponse = "{\"b\":{}}"
  }

  def probe(implicit system: ActorSystem) = {
    val probe = TestProbe()

    probe.setAutoPilot {
      new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any) = msg match {
          case GetMuscleGroups =>
            sender ! TestData.muscleGroups
            TestActor.KeepRunning
          case UserExerciseSessionStart(_, _) =>
            sender ! \/-(TestData.userId.id)
            TestActor.KeepRunning
          case UserGetExerciseSessionsSummary(_, _, _) =>
            sender ! TestData.sessionSummary
            TestActor.KeepRunning
          case UserGetExerciseSession(_, _) =>
            sender ! TestData.session
            TestActor.KeepRunning
          case UserExerciseDataProcessSinglePacket(_, _, _) =>
            sender ! \/.right(())
            TestActor.KeepRunning
          case UserExerciseDataProcessMultiplePackets(_, _, _) =>
            sender ! \/.right(())
            TestActor.KeepRunning
          case UserGetExerciseSessionsDates(_) =>
            sender ! TestData.sessionDates
            TestActor.KeepRunning
          case UserExerciseSessionEnd(_, _) =>
            sender ! \/.right(())
            TestActor.KeepRunning
          case UserExerciseClassify(_, _, _, _) =>
            sender ! \/.right(())
            TestActor.KeepRunning
        }
      }
    }

    probe
  }
}

class ExerciseServiceTest
  extends FlatSpec
  with ScalatestRouteTest
  with TestKitBase
  with Matchers
  with ExerciseService
  with ExerciseMarshallers
  with LiftTestMarshallers
  with ImplicitSender {

  import com.eigengo.lift.exercise.ExerciseServiceTest._

  val probe = ExerciseServiceTest.probe

  val underTest = exerciseRoute(probe.ref, probe.ref, probe.ref)

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  "The Exercise service" should "listen at GET /exercise/musclegroups endpoint" in {
    Get("/exercise/musclegroups") ~> underTest ~> check {
      responseAs[List[MuscleGroup]] should be(TestData.muscleGroups)
    }

    probe.expectMsg(GetMuscleGroups)
  }

  it should "listen at POST exercise/UserIdValue endpoint" in {
    Post(s"/exercise/${TestData.userId.id}", TestData.sessionProps) ~> underTest ~> check {
      UUID.fromString(response.entity.asString.replace("\"", "")) should be(TestData.userId.id)
    }

    probe.expectMsg(UserExerciseSessionStart(TestData.userId, TestData.sessionProps))
  }

  it should "listen at GET exercise/UserIdValue?startDate=date&endDate=date endpoint" in {
    Get(s"/exercise/${TestData.userId.id}?startDate=${dateFormat.format(TestData.startDate)}&endDate=${dateFormat.format(TestData.endDate)}") ~> underTest ~> check {
      responseAs[List[SessionSummary]].head.sessionProps should be(TestData.sessionSummary.head.sessionProps)
    }

    probe.expectMsg(UserGetExerciseSessionsSummary(TestData.userId, TestData.startDate, TestData.endDate))
  }

  it should "listen at GET exercise/UserIdValue?date=date endpoint" in {
    Get(s"/exercise/${TestData.userId.id}?date=${dateFormat.format(TestData.startDate)}") ~> underTest ~> check {
      responseAs[List[SessionSummary]].head.sessionProps should be(TestData.sessionSummary.head.sessionProps)
    }

    probe.expectMsg(UserGetExerciseSessionsSummary(TestData.userId, TestData.startDate, TestData.startDate))
  }

  it should "listen at GET exercise/UserIdValue endpoint" in {
    Get(s"/exercise/${TestData.userId.id}") ~> underTest ~> check {
      responseAs[List[SessionDate]] should be(TestData.sessionDates)
    }

    probe.expectMsg(UserGetExerciseSessionsDates(TestData.userId))
  }

  it should "listen at GET exercise/UserIdValue/SessionIdValue endpoint" in {
    Get(s"/exercise/${TestData.userId.id}/${TestData.sessionId.id}") ~> underTest ~> check {
      responseAs[Option[ExerciseSession]] should be(TestData.session)
    }

    probe.expectMsg(UserGetExerciseSession(TestData.userId, TestData.sessionId))
  }

  it should "listen at PUT exercise/UserIdValue/SessionIdValue endpoint" in {
    Put(s"/exercise/${TestData.userId.id}/${TestData.sessionId.id}", TestData.bitVector) ~> underTest ~> check {
      response.entity.asString should be(TestData.emptyResponse)
    }

    probe.expectMsg(UserExerciseDataProcessSinglePacket(TestData.userId, TestData.sessionId, TestData.bitVector))
  }

  it should "listen at DELETE exercise/UserIdValue/SessionIdValue endpoint" in {
    Delete(s"/exercise/${TestData.userId.id}/${TestData.sessionId.id}") ~> underTest ~> check {
      response.entity.asString should be(TestData.emptyResponse)
    }

    probe.expectMsg(UserExerciseSessionEnd(TestData.userId, TestData.sessionId))
  }

  it should "listen at POST exercise/UserIdValue/SessionIdValue/classification endpoint" in {
    Post(s"/exercise/${TestData.userId.id}/${TestData.sessionId.id}/classification", Exercise(TestData.squat, TestData.intensity)) ~> underTest ~> check {
      response.entity.asString should be(TestData.emptyResponse)
    }

    probe.expectMsg(UserExerciseClassify(TestData.userId, TestData.sessionId, TestData.squat, TestData.intensity))
  }
}
