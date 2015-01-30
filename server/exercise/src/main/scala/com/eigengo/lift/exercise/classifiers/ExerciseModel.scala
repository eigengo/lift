package com.eigengo.lift.exercise.classifiers

import akka.actor.ActorRef
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.eigengo.lift.exercise.{SensorData, SessionProperties}
import scalaz.\/

object ExerciseModel {

  /**
   * Language used to query exercise models with. Currently we encode a linear-time temporal logic DSL.
   *
   * NOTE: models are responsible for interpreting query semantics in a meaningful way!
   */
  sealed trait Query {
    /**
     * Defines the properties associated with an exercise session. This value is typically implicitly defined within the
     * context in which the query is built.
     */
    def session: SessionProperties
  }

  /**
   * Query that always succeeds
   */
  case class True(implicit val session: SessionProperties) extends Query

  /**
   * Query that should be true at all future points in time
   *
   * @param query query that is to be true at all future points in time
   */
  case class Box(query: Query)(implicit val session: SessionProperties) extends Query

  /**
   * Query that should be true at some point in the future
   *
   * @param query query that is to be true at some point in the future
   */
  case class Diamond(query: Query)(implicit val session: SessionProperties) extends Query

}

/**
 * Model interface trait. Implementations of this trait define specific exercising models that may be updated and queried.
 */
trait ExerciseModel {

  import ExerciseModel._

  /**
   * Unique name for exercise model
   */
  def name: String

  /**
   * Result type that queries are evaluated to
   */
  type Result

  /**
   * Updates the model by adding in new sensor event data.
   *
   * @param event newly received sensor event data
   */
  def update[A <: SensorData](event: ClassifyExerciseEvt[A]): Unit

  /**
   * Evaluates a query to either an error value or a result value
   *
   * @param formula formula defining the query to be evaluated
   * @return        result of evaluating the query formula
   */
  def evaluate(formula: Query): String \/ Result

  /**
   * Queries the model by evaluating a given query. Query result is then sent (as a message) to `listener`.
   *
   * @param formula  formula defining the query
   * @param listener Actor that will receive model query result
   */
  def query(formula: Query, listener: ActorRef): Unit = {
    listener ! evaluate(formula)
  }

}
