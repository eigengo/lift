package com.eigengo.lift.exercise.classifiers

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions.Fact
import com.eigengo.lift.exercise.{SensorData, SessionProperties}
import scalaz.{-\/, \/-, \/}

object ExerciseModel {

  trait Path

  /**
   * Language used to query exercise models with. Currently we encode a DSL for linear-time dynamic logic. All temporal
   * operators are past tense - we do this since we only know the truth of a sequence after we have scanned it.
   *
   * NOTE: exercise models are responsible for interpreting query language semantics in a meaningful way!
   */
  sealed trait Query

  /**
   * Query that expects the given assertion to be true in the current state
   *
   * @param fact assertion that should hold in the current state
   */
  case class Atomic(fact: Fact) extends Query

  case class `&&`(query1: Query, query2: Query, remaining: Query*) extends Query

  case class `||`(query1: Query, query2: Query, remaining: Query*) extends Query

  /**
   * Query that should be true in the last state or point in time
   *
   * @param query query that is to be true in the last state
   */
  case class `<>`(path: Path, query: Query) extends Query

  case class `[]`(path: Path, query: Query) extends Query

  /**
   * Message sent to exercise model actor. Indicates that model is to be updated and the model's `watch` queries are to
   * be checked for satisfiability.
   */
  case class Update[A <: SensorData](event: ClassifyExerciseEvt[A])

}

/**
 * Model interface trait. Implementations of this trait define specific exercising models that may be updated and queried.
 * Querying determines the states or points in time at which a `watch` query is satisfiable.
 */
trait ExerciseModel[R] {
  this: Actor with ActorLogging =>

  import ExerciseModel._

  /**
   * Unique name for exercise model
   */
  def name: String

  /**
   * Session properties for this model
   */
  def sessionProps: SessionProperties

  /**
   * Collection of queries that are to be evaluated/checked for each update message that we receive
   */
  def watch: Set[Query]

  /**
   * Evaluates a query to either an error/false (truth) value or a positive/true (truth) value
   *
   * @param formula formula defining the query to be evaluated
   * @return        result of evaluating the query formula
   */
  def evaluate(formula: Query): Option[String] \/ R

  def receive = LoggingReceive {
    case Update(event) =>
      watch.foreach { query =>
        evaluate(query) match {
          case \/-(result) =>
            sender() ! result

          case -\/(None) =>
            // we ignore this case as query evaluated to be false without any error

          case -\/(Some(err)) =>
            log.error(err)
        }
      }
  }

}
