package com.eigengo.lift.exercise.classifiers

import akka.actor.ActorRef
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.eigengo.lift.exercise.{SensorData, SessionProperties}

object ExerciseModel {

  /**
   * Language used to query exercise models with
   */
  sealed trait Query {
    def session: SessionProperties
  }

  /**
   * Query that always succeeds
   */
  case class True(session: SessionProperties) extends Query

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
   * Updates the model by adding in new sensor event data.
   *
   * @param event newly received sensor event data
   */
  def update[A <: SensorData](event: ClassifyExerciseEvt[A]): Unit

  /**
   * Queries the model. Query result is sent (as a message) to `listener`.
   *
   * @param formula  formula defining the query
   * @param listener Actor that will receive model query result
   */
  def query(formula: Query, listener: ActorRef): Unit

}
