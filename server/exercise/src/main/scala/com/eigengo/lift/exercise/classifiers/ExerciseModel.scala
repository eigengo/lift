package com.eigengo.lift.exercise.classifiers

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import com.eigengo.lift.exercise.UserExercises.ClassifyExerciseEvt
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions.{Assertion, Bind, True}
import com.eigengo.lift.exercise.{SensorData, SessionProperties}
import scalaz.{-\/, \/-, \/}

object ExerciseModel {

  /**
   * We query exercise models using a DSL based upon a linear-time dynamic logic. Exercise sessions define the finite
   * length trace over which our queries will be evaluated. Paths are used to define (logical) windows over which queries
   * are to hold.
   *
   * NOTE:
   *   1. we intentionally have negation as being defined (rather than as a language primitive) - that way queries et al
   *      may be kept in negation normal form (NNF).
   *   2. exercise models are responsible for interpreting query language semantics in a meaningful way!
   *
   * REFERENCE:
   *   [2014] LTLf and LDLf Monitoring by Giuseppe De Giacomo, Riccardo De Masellis, Marco Grasso, Fabrizio Maria Maggi
   *   and Marco Montali
   */

  /**
   * Path language - we encode path regular expressions here
   */
  sealed trait Path

  case class Assert(assertion: Assertion) extends Path

  case class Test(query: Query) extends Path

  case class Choice(path1: Path, path2: Path, remaining: Path*) extends Path

  case class Seq(path1: Path, path2: Path, remaining: Path*) extends Path

  case class Repeat(path: Path) extends Path

  /**
   * Auxillary function that determines if a path only involves combinations of `Test` expressions (used by standard
   * model).
   *
   * @param path path to be tested
   */
  def testOnly(path: Path): Boolean = path match {
    case Assert(_) =>
      false

    case Test(_) =>
      true

    case Choice(path1, path2, remainingPaths @ _*) =>
      (path1 +: path2 +: remainingPaths).map(testOnly).fold(true) { case (x, y) => x && y }

    case Seq(path1, path2, remainingPaths @ _*) =>
      (path1 +: path2 +: remainingPaths).map(testOnly).fold(true) { case (x, y) => x && y }

    case Repeat(path1) =>
      testOnly(path1)
  }

  /**
   * Query language - we encode linear-time dynamic logic here
   */
  sealed trait Query

  case class Formula(assertion: Assertion) extends Query

  case object TT extends Query

  case object FF extends Query

  case class And(query1: Query, query2: Query, remainingQueries: Query*) extends Query

  case class Or(query1: Query, query2: Query, remainingQueries: Query*) extends Query

  /**
   * Logical expressions that operate on path prefixes:
   *   - `Exists` asserts existence of a path prefix;
   *   - `All` asserts query for all path prefixes.
   *
   * @param path  path prefix at end of which query should hold
   * @param query query that is to hold at end of a path prefix
   */
  case class Exists(path: Path, query: Query) extends Query

  case class All(path: Path, query: Query) extends Query

  /**
   * Convenience function that provides negation on queries, whilst keeping them in NNF. Translation is linear in the
   * size of the query.
   */
  def not(query: Query): Query = query match {
    case Formula(assertion) =>
      Formula(ClassificationAssertions.not(assertion))

    case TT =>
      FF

    case FF =>
      TT

    case And(query1, query2, remaining @ _*) =>
      Or(not(query1), not(query2), remaining.map(not): _*)

    case Or(query1, query2, remaining @ _*) =>
      And(not(query1), not(query2), remaining.map(not): _*)

    case Exists(path, query1) =>
      All(path, not(query1))

    case All(path, query1) =>
      Exists(path, not(query1))
  }

  /**
   * Indicates that the exercise session has completed (remaining trace is empty)
   */
  val End: Query = All(Test(Formula(True)), FF)

  /**
   * Denotes the last step of the exercise session
   */
  val Last: Query = Exists(Assert(True), End)

  /**
   * Following definitions allow linear-time logic to be encoded within the current logic. Translation here is linear in
   * the size of the formula.
   */

  /**
   * At the next point of the exercise session, the query will hold
   */
  def Next(query: Query): Query = Exists(Assert(True), query)

  /**
   * At some point in the exercise session, the query will hold
   */
  def Diamond(query: Query): Query = Exists(Repeat(Assert(True)), query)

  /**
   * For all points in the exercise session, the query holds
   */
  def Box(query: Query): Query = All(Repeat(Assert(True)), query)

  /**
   * Until query2 holds, query1 will hold in the exercise session. Query2 will hold at some point during the exercise
   * session.
   */
  def Until(query1: Query, query2: Query): Query = Exists(Repeat(Seq(Test(query1), Assert(True))), query2)

  /**
   * Message sent to exercise model actor. Indicates that model is to be updated and the model's `watch` queries are to
   * be checked for satisfiability.
   */
  case class Update[A <: SensorData](event: ClassifyExerciseEvt[A])

  /**
   * Values representing the current evaluation state of a given query:
   *   - stable queries are values that hold now and, no matter how the model develops, will remain in their current state
   *   - unstable queries are values that hold now and, for some sequence of possible events updates, may deviate from
   *     their current value
   */
  sealed trait QueryValue {
    /**
     * The actual query result or value
     */
    def result: Boolean
  }
  case class StableValue(result: Boolean) extends QueryValue
  case class UnstableValue(result: Boolean) extends QueryValue

  /**
   * Auxillary functions that support QueryValue lattice structure
   */

  def meet(value1: QueryValue, value2: QueryValue): QueryValue = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 && result2)

    case (UnstableValue(result1), UnstableValue(result2)) =>
      UnstableValue(result1 && result2)

    case (StableValue(true), result2 @ UnstableValue(_)) =>
      result2

    case (result1 @ StableValue(false), UnstableValue(_)) =>
      result1

    case (result1 @ UnstableValue(_), StableValue(true)) =>
      result1

    case (UnstableValue(_), result2 @ StableValue(false)) =>
      result2
  }

  def join(value1: QueryValue, value2: QueryValue): QueryValue = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 || result2)

    case (UnstableValue(result1), UnstableValue(result2)) =>
      UnstableValue(result1 || result2)

    case (result1 @ StableValue(true), UnstableValue(_)) =>
      result1

    case (StableValue(false), result2 @ UnstableValue(_)) =>
      result2

    case (UnstableValue(_), result2 @ StableValue(true)) =>
      result2

    case (result1 @ UnstableValue(_), StableValue(false)) =>
      result1
  }

  def complement(value: QueryValue): QueryValue = value match {
    case StableValue(result) =>
      StableValue(!result)

    case UnstableValue(result) =>
      UnstableValue(!result)
  }

  /**
   * Result message - returned to sender as model evaluates `watch` queries.
   *
   * @param query query that triggered this response message
   * @param value result or outcome of model evaluation
   */
  case class QueryResult(query: Query, value: QueryValue)

}

/**
 * Model interface trait. Implementations of this trait define specific exercising models that may be updated and queried.
 * Querying determines the states or points in time at which a `watch` query is satisfiable.
 */
trait ExerciseModel {
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
  def positiveWatch: Set[Query]
  def negativeWatch: Set[Query]

  /**
   * Evaluates a query to either an error/false (truth) value or a positive/true (truth) value
   *
   * @param formula formula defining the query to be evaluated
   * @param current current state (at which formula is to be evaluated) - if end of exercise session, then `None`
   * @param next    next or following state - if no state follows, then `None`
   * @return        result of evaluating the query formula
   */
  def evaluate[A](formula: Query)(current: Bind[A], next: Option[Bind[A]]): QueryValue

  def receive = LoggingReceive {
    // Trait implementations determine how the model is updated with the new event
    case Update(event) =>
      (positiveWatch ++ negativeWatch).foreach { query =>
        val value = evaluate(query)(???, ???) // FIXME: need to extract state information from SlidingWindow(2)!

        if (value.result) {
          if (positiveWatch.contains(query)) {
            sender() ! QueryResult(query, value)
          }
        } else {
          if (negativeWatch.contains(query)) {
            sender() ! QueryResult(query, value)
          }
        }
      }
  }

}
