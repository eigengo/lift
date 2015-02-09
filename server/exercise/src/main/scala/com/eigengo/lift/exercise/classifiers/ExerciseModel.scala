package com.eigengo.lift.exercise.classifiers

import akka.actor.{ActorRef, ActorLogging}
import akka.event.LoggingReceive
import akka.stream.{OverflowStrategy, ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorSubscriber
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.WatermarkRequestStrategy
import akka.stream.scaladsl._
import com.eigengo.lift.exercise.classifiers.workflows.{SlidingWindow, ClassificationAssertions}
import com.eigengo.lift.exercise.{SensorDataSourceLocation, Sensor, SensorData, SessionProperties}

object ExerciseModel {

  import ClassificationAssertions._

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

  sealed trait SensorQuery
  case object AllSensors extends SensorQuery
  case object SomeSensor extends SensorQuery
  case class NamedSensor(name: SensorDataSourceLocation) extends SensorQuery

  def not(sensor: SensorQuery): SensorQuery = sensor match {
    case AllSensors =>
      SomeSensor

    case SomeSensor =>
      AllSensors

    case _ =>
      sensor
  }

  /**
   * Path language - we encode path regular expressions here
   */
  sealed trait Path

  case class Assert(sensor: SensorQuery, fact: Fact) extends Path // FIXME: what about the full range of propositional formulae?

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
    case Assert(_, _) =>
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

  case class Formula(sensor: SensorQuery, fact: Fact) extends Query

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
    case Formula(sensor, assertion) =>
      Formula(not(sensor), ClassificationAssertions.not(assertion))

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
   * Convenience function that determines if a query is propositional (i.e. contains no dynamic path subformula).
   */
  def propositional(query: Query): Boolean = query match {
    case Exists(_, _) | All(_, _) =>
      false

    case Formula(_, _) | TT | FF =>
      true

    case And(query1, query2, remaining @ _*) =>
      val result = (query1 +: query2 +: remaining).map(propositional)
      result.forall(_ => true)

    case Or(query1, query2, remaining @ _*) =>
      val result = (query1 +: query2 +: remaining).map(propositional)
      result.forall(_ => true)
  }

  /**
   * Indicates that the exercise session has completed (remaining trace is empty)
   */
  val End: Query = All(Test(Formula(SomeSensor, True)), FF)

  /**
   * Denotes the last step of the exercise session
   */
  val Last: Query = Exists(Assert(SomeSensor, True), End)

  /**
   * Following definitions allow linear-time logic to be encoded within the current logic. Translation here is linear in
   * the size of the formula.
   */

  /**
   * At the next point of the exercise session, the query will hold
   */
  def Next(query: Query): Query = Exists(Assert(SomeSensor, True), query)

  /**
   * At some point in the exercise session, the query will hold
   */
  def Diamond(query: Query): Query = Exists(Repeat(Assert(SomeSensor, True)), query)

  /**
   * For all points in the exercise session, the query holds
   */
  def Box(query: Query): Query = All(Repeat(Assert(AllSensors, True)), query)

  /**
   * Until query2 holds, query1 will hold in the exercise session. Query2 will hold at some point during the exercise
   * session.
   */
  def Until(query1: Query, query2: Query): Query = Exists(Repeat(Seq(Test(query1), Assert(SomeSensor, True))), query2)

  /**
   * Message sent to exercise model actor. Indicates that model is to be updated and the model's `watch` queries are to
   * be checked for satisfiability.
   */
  case class Update[A <: SensorData](event: Map[SensorDataSourceLocation, A]) {
    require(event.keySet == Sensor.sourceLocations)
  }

  /**
   * Internal actor message. Allows the evaluator to run after a model update has occurred.
   *
   * @param next      the next (state) event received - enriched with propositional facts that hold in this state
   * @param lastState flag indicating if the exercise session stream will close (i.e. we are the last state)
   * @param listener  actor that receives callbacks on watched formulae
   */
  case class NextState[A <: SensorData](next: Map[SensorDataSourceLocation, Bind[A]], lastState: Boolean, listener: ActorRef)

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
  case class UnstableValue(result: Boolean, state: Query) extends QueryValue

  /**
   * Auxillary functions that support QueryValue lattice structure
   */

  def meet(value1: QueryValue, value2: QueryValue): QueryValue = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 && result2)

    case (UnstableValue(result1, atom1), UnstableValue(result2, atom2)) =>
      UnstableValue(result1 && result2, And(atom1, atom2))

    case (StableValue(true), result2 @ UnstableValue(_, _)) =>
      result2

    case (result1 @ StableValue(false), UnstableValue(_, _)) =>
      result1

    case (result1 @ UnstableValue(_, _), StableValue(true)) =>
      result1

    case (UnstableValue(_, _), result2 @ StableValue(false)) =>
      result2
  }

  def join(value1: QueryValue, value2: QueryValue): QueryValue = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 || result2)

    case (UnstableValue(result1, atom1), UnstableValue(result2, atom2)) =>
      UnstableValue(result1 || result2, Or(atom1, atom2))

    case (result1 @ StableValue(true), UnstableValue(_, _)) =>
      result1

    case (StableValue(false), result2 @ UnstableValue(_, _)) =>
      result2

    case (UnstableValue(_, _), result2 @ StableValue(true)) =>
      result2

    case (result1 @ UnstableValue(_, _), StableValue(false)) =>
      result1
  }

  def complement(value: QueryValue): QueryValue = value match {
    case StableValue(result) =>
      StableValue(!result)

    case UnstableValue(result, atom) =>
      UnstableValue(!result, not(atom))
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
trait ExerciseModel[A <: SensorData] extends ActorPublisher[(Map[SensorDataSourceLocation, A], ActorRef)] with ActorSubscriber {
  this: ActorLogging =>

  import ClassificationAssertions.Bind
  import ExerciseModel._
  import FlowGraphImplicits._

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

  protected def workflow: Flow[Map[SensorDataSourceLocation, A], Map[SensorDataSourceLocation, Bind[A]]]

  val config = context.system.settings.config

  val settings = ActorFlowMaterializerSettings(context.system)
  // Received sensor data is buffered
  val bufferSize = config.getInt("classification.buffer")

  implicit val materializer = ActorFlowMaterializer(settings)

  override val requestStrategy = WatermarkRequestStrategy(config.getInt("classification.watermark"))

  override def preStart() = {
    FlowGraph { implicit builder =>
      val split = Unzip[Map[SensorDataSourceLocation, A], ActorRef]
      val join = Zip[List[Map[SensorDataSourceLocation, Bind[A]]], ActorRef]

      Source(ActorPublisher(self)) ~> Flow[(Map[SensorDataSourceLocation, A], ActorRef)].buffer(bufferSize, OverflowStrategy.dropHead) ~> split.in
      split.left ~> workflow.transform(() => SlidingWindow[Map[SensorDataSourceLocation, Bind[A]]](2)) ~> join.left
      split.right ~> join.right
      join.out ~> Flow[(List[Map[SensorDataSourceLocation, Bind[A]]], ActorRef)].map {
        case (List(event), listener) =>
          NextState(event, lastState = true, listener)

        case (List(event, _), listener) =>
          NextState(event, lastState = false, listener)
      } ~> Sink(ActorSubscriber[NextState[A]](self))
    }.run()
  }

  /**
   * Evaluates a query to either an error/false (truth) value or a positive/true (truth) value
   *
   * @param formula   formula defining the query to be evaluated
   * @param current   current state (at which formula is to be evaluated) - if end of exercise session, then `None`
   * @param lastState next or following state - if no state follows, then `None`
   * @return          result of evaluating the query formula
   */
  def evaluate(formula: Query)(current: Map[SensorDataSourceLocation, Bind[A]], lastState: Boolean): QueryValue

  protected def modelUpdate: Receive = {
    case Update(event: Map[SensorDataSourceLocation, A]) =>
      if (isActive && totalDemand > 0) {
        onNext((event, sender()))
      } else if (isActive) {
        log.error(s"Actor publisher is inactive and we received an Update event, so dropping $event")
      } else {
        log.warning(s"No demand for the actor publisher and we received an Update event, so dropping $event")
      }
  }

  protected def monitorEvents: Receive = {
    case OnNext(NextState(next: Map[SensorDataSourceLocation, Bind[A]], lastState, listener)) =>
      (positiveWatch ++ negativeWatch).foreach { query =>
        val value = evaluate(query)(next, lastState)

        if (value.result) {
          if (positiveWatch.contains(query)) {
            listener ! QueryResult(query, value)
          }
        } else {
          if (negativeWatch.contains(query)) {
            listener ! QueryResult(query, value)
          }
        }
      }
  }

  def receive = LoggingReceive { modelUpdate orElse monitorEvents }

}
