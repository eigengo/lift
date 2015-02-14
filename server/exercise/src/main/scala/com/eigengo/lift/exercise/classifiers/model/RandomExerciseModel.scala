package com.eigengo.lift.exercise.classifiers.model

import akka.actor.{ActorLogging, Actor}
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.UserExercises.ModelMetadata
import com.eigengo.lift.exercise.UserExercisesClassifier.{UnclassifiedExercise, FullyClassifiedExercise}
import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions
import scala.collection.parallel.mutable
import scala.concurrent.Future
import scala.util.Random

/**
 * Random exercising model. Updates are simply printed out and queries always succeed (by sending a random message to
 * the listening actor).
 */
class RandomExerciseModel(val sessionProps: SessionProperties)
  extends ExerciseModel
  with SMTInterface
  with Actor
  with ActorLogging {

  import ClassificationAssertions._
  import context.dispatcher
  import ExerciseModel._

  val name = "random"

  private val exercises =
    Map(
      "arms" → List("Biceps curl", "Triceps press"),
      "chest" → List("Chest press", "Butterfly", "Cable cross-over")
    )

  // For the random model, we watch and report on all exercises and all sensors
  val watch = mutable.ParTrieMap(exercises.values.flatMap(_.flatMap(t => Sensor.sourceLocations.map(l => Formula(Assert(l, Gesture(t, 0.80)))))).map((f: Query) => (f, f)).toSeq: _*)

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

  // Random model performs no query simplification
  def simplify(query: Query) = Future(query)

  // Random model always claims that query is satisfiable
  def satisfiable(query: Query) = Future(true)

  // Random model evaluator always returns true!
  def evaluateQuery(query: Query)(current: BindToSensors, lastState: Boolean) =
    StableValue(result = true)

  def makeDecision(value: QueryResult) =
    if (value.result) {
      val exercise = (value.query: @unchecked) match {
        case Formula(Assert(_, Gesture(nm, _))) =>
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
        case (location, AccelerometerData(sr, values)) =>
          val xs = values.map(_.x)
          val ys = values.map(_.y)
          val zs = values.map(_.z)
          println(s"****** Acceleration $location | X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")

        case (location, RotationData(_, values)) =>
          val xs = values.map(_.x)
          val ys = values.map(_.y)
          val zs = values.map(_.z)
          println(s"****** Rotation $location | X: (${xs.min}, ${xs.max}), Y: (${ys.min}, ${ys.max}), Z: (${zs.min}, ${zs.max})")
      }}
      super.aroundReceive(receive, msg)

    case _ =>
      super.aroundReceive(receive, msg)
  }

}
