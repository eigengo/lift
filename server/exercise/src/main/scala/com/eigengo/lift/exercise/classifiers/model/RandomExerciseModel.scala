package com.eigengo.lift.exercise.classifiers.model

import akka.actor.{ActorLogging, Actor}
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.UserExercises.ModelMetadata
import com.eigengo.lift.exercise.UserExercisesClassifier.{UnclassifiedExercise, FullyClassifiedExercise}
import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel._
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object RandomExerciseModel {
  val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )

  implicit val prover = new SMTInterface {
    // Random model performs no query simplification
    def simplify(query: Query)(implicit ec: ExecutionContext) = Future(query)

    // Random model always claims that query is satisfiable
    def satisfiable(query: Query)(implicit ec: ExecutionContext) = Future(true)

    // Random model always claims that query is valid
    def valid(query: Query)(implicit ec: ExecutionContext) = Future(true)
  }
}

/**
 * Random exercising model. Updates are simply printed out and queries always succeed (by sending a random message to
 * the listening actor).
 */
class RandomExerciseModel(sessionProps: SessionProperties)
  extends ExerciseModel("random", sessionProps, for (sensor <- Sensor.sourceLocations; exercise <- RandomExerciseModel.exercises.values.flatten) yield Formula(Assert(Gesture(exercise, 0.80), sensor)))(RandomExerciseModel.prover)
  with Actor
  with ActorLogging {

  import RandomExerciseModel._

  private val metadata = ModelMetadata(2)

  private def randomExercise(): Set[Fact] = {
    val mgk = Random.shuffle(sessionProps.muscleGroupKeys).head
    if (exercises.get(mgk).isEmpty) {
      Set.empty
    } else {
      val exerciseType = Random.shuffle(exercises.get(mgk).get).head

      Set(Gesture(exerciseType, 0.80))
    }
  }

  // Workflow simply adds random facts to random sensors
  val workflow =
    Flow[SensorNetValue]
      .map { sn =>
        val classification = randomExercise()
        val sensor = Random.shuffle(sn.toMap.keys).head

        BindToSensors(sn.toMap.map { case (location, _) => if (location == sensor) (location, classification) else (location, Set.empty[Fact]) }.toMap, sn)
      }

  // Random model evaluator always returns true!
  def evaluateQuery(query: Query)(current: BindToSensors, lastState: Boolean) =
    StableValue(result = true)

  def makeDecision(query: Query, value: QueryValue, result: Boolean) =
    if (result) {
      val exercise = (query: @unchecked) match {
        case Formula(Assert(Gesture(nm, _), _)) =>
          Exercise(nm, None, None)
      }

      FullyClassifiedExercise(metadata, 1.0, exercise)
    } else {
      UnclassifiedExercise(metadata)
    }

  /**
   * We use `aroundReceive` here to print out a summary `SensorNet` message.
   */
  override def aroundReceive(receive: Receive, msg: Any) = msg match {
    case event: SensorNet =>
      event.toMap.foreach { x => (x: @unchecked) match {
        case (location, data: Vector[_]) =>
          for ((AccelerometerData(_, values), point) <- data.zipWithIndex) {
            val xs = values.map(_.x)
            val ys = values.map(_.y)
            val zs = values.map(_.z)
            println(s"****** Acceleration $location@$point | X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
          }
          for ((RotationData(_, values), point) <- data.zipWithIndex) {
            val xs = values.map(_.x)
            val ys = values.map(_.y)
            val zs = values.map(_.z)
            println(s"****** Rotation $location@$point | X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
          }
      }}
      super.aroundReceive(receive, msg)

    case _ =>
      super.aroundReceive(receive, msg)
  }

}
