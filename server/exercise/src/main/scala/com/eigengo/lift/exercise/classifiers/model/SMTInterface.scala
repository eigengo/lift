package com.eigengo.lift.exercise.classifiers.model

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import scala.concurrent.Future

trait SMTInterface {

  import ExerciseModel._

  /**
   * Function that treats the query as a propositional formula (so, path expressions are taken to be "propositional")
   * and rewrites it by simplifying it.
   *
   * @param query query to be rewritten/simplified by applying propositional rules of reasoning
   */
  def simplify(query: Query): Future[Query]

  /**
   * Function that interacts with an SMT prover and determines if the query is satisfiable or not.
   *
   * @param query LDL formula that is treated as being propositional
   */
  def satisfiable(query: Query): Future[Boolean]

}
