package com.eigengo.lift.exercise.classifiers

import akka.actor.{ActorRef, ActorLogging}
import akka.event.LoggingReceive
import akka.stream.{OverflowStrategy, ActorFlowMaterializer, ActorFlowMaterializerSettings}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import scala.async.Async._
import com.eigengo.lift.exercise.UserExercisesClassifier.ClassifiedExercise
import com.eigengo.lift.exercise.classifiers.model.SMTInterface
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions.BindToSensors
import com.eigengo.lift.exercise.classifiers.workflows.{SlidingWindow, ClassificationAssertions}
import com.eigengo.lift.exercise._
import scala.collection.parallel.mutable
import scala.concurrent.Future
import scala.language.higherKinds

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

  sealed trait Proposition

  case class Assert(sensor: SensorDataSourceLocation, fact: Fact) extends Proposition

  case class Conjunction(fact1: Proposition, fact2: Proposition, remainingFacts: Proposition*) extends Proposition

  case class Disjunction(fact1: Proposition, fact2: Proposition, remainingFacts: Proposition*) extends Proposition

  def not(fact: Proposition): Proposition = fact match {
    case Assert(sensor, fact1) =>
      Assert(sensor, ClassificationAssertions.not(fact1))

    case Conjunction(fact1, fact2, remaining @ _*) =>
      Disjunction(not(fact1), not(fact2), remaining.map(not): _*)

    case Disjunction(fact1, fact2, remaining @ _*) =>
      Conjunction(not(fact1), not(fact2), remaining.map(not): _*)
  }

  /**
   * Path language - we encode path regular expressions here
   */
  sealed trait Path

  case class AssertFact(fact: Proposition) extends Path

  case class Test(query: Query) extends Path

  case class Choice(path1: Path, path2: Path, remaining: Path*) extends Path

  case class Sequence(path1: Path, path2: Path, remaining: Path*) extends Path

  case class Repeat(path: Path) extends Path

  /**
   * Auxillary function that determines if a path only involves combinations of `Test` expressions (used by standard
   * model).
   *
   * @param path path to be tested
   */
  def testOnly(path: Path): Boolean = path match {
    case AssertFact(_) =>
      false

    case Test(_) =>
      true

    case Choice(path1, path2, remainingPaths @ _*) =>
      (path1 +: path2 +: remainingPaths).map(testOnly).fold(true) { case (x, y) => x && y }

    case Sequence(path1, path2, remainingPaths @ _*) =>
      (path1 +: path2 +: remainingPaths).map(testOnly).fold(true) { case (x, y) => x && y }

    case Repeat(path1) =>
      testOnly(path1)
  }

  /**
   * Query language - we encode linear-time dynamic logic here
   */
  sealed trait Query

  case class Formula(fact: Proposition) extends Query

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
    case Formula(fact) =>
      Formula(not(fact))

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
  def End(location: SensorDataSourceLocation): Query = All(Test(Formula(Assert(location, True))), FF)

  /**
   * Denotes the last step of the exercise session
   */
  def Last(location: SensorDataSourceLocation): Query = Exists(AssertFact(Assert(location, True)), End(location))

  /**
   * Following definitions allow linear-time logic to be encoded within the current logic. Translation here is linear in
   * the size of the formula.
   */

  /**
   * At the next point of the exercise session, the query will hold
   */
  def Next(location: SensorDataSourceLocation, query: Query): Query = Exists(AssertFact(Assert(location, True)), query)

  /**
   * At some point in the exercise session, the query will hold
   */
  def Diamond(location: SensorDataSourceLocation, query: Query): Query = Exists(Repeat(AssertFact(Assert(location, True))), query)

  /**
   * For all points in the exercise session, the query holds
   */
  def Box(location: SensorDataSourceLocation, query: Query): Query = All(Repeat(AssertFact(Assert(location, True))), query)

  /**
   * Until query2 holds, query1 will hold in the exercise session. Query2 will hold at some point during the exercise
   * session.
   */
  def Until(location: SensorDataSourceLocation, query1: Query, query2: Query): Query = Exists(Repeat(Sequence(Test(query1), AssertFact(Assert(location, True)))), query2)

  /**
   * Values representing the current evaluation state of a given query:
   *   - stable queries are values that hold now and, no matter how the model develops, will remain in their current state
   *   - unstable queries are values that hold now and, for some sequence of possible events updates, may deviate from
   *     their current value
   */
  sealed trait QueryValue
  /**
   * @param result validity of linear dynamic logic statement at this and future points in time
   */
  case class StableValue(result: Boolean) extends QueryValue
  /**
   * @param state  positive propositional description of the next states for an alternating automaton over words
   */
  case class UnstableValue(state: Query) extends QueryValue

  /**
   * Auxillary functions that support QueryValue lattice structure
   */

  def meet(value1: QueryValue, value2: QueryValue): QueryValue = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 && result2)

    case (UnstableValue(atom1), UnstableValue(atom2)) =>
      UnstableValue(And(atom1, atom2))

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

    case (UnstableValue(atom1), UnstableValue(atom2)) =>
      UnstableValue(Or(atom1, atom2))

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

    case UnstableValue(atom) =>
      UnstableValue(not(atom))
  }

}

/**
 * Exercising model interface. Implementations of this abstract class define specific exercising models that may be updated and queried.
 * Querying determines the states or points in time at which a `watch` query is satisfiable.
 */
abstract class ExerciseModel(name: String, sessionProps: SessionProperties, toWatch: Set[ExerciseModel.Query] = Set.empty)
  extends ActorPublisher[(SensorNetValue, ActorRef)]
  with ActorLogging {

  this: SMTInterface =>

  import context.dispatcher
  import ExerciseModel._
  import FlowGraphImplicits._

  val config = context.system.settings.config
  val settings = ActorFlowMaterializerSettings(context.system)
  // Received sensor data is buffered - configuration data determines buffer size
  val bufferSize = config.getInt("classification.buffer")
  val samplingRate = config.getInt("classification.frequency")

  implicit val materializer = ActorFlowMaterializer(settings)

  /**
   * Collection of queries that are to be evaluated/checked for each update message that we receive.
   *
   * NOTE: mutable map may get accessed concurrently from separate evaluation workflows
   */
  private val watch = mutable.ParMap.empty[Query, Query]
  /**
   * Collection of queries that a stable result value has been evaluated for.
   *
   * NOTE: mutable map may get accessed concurrently from separate evaluation workflows
   */
  private val stableQuery = mutable.ParMap.empty[Query, QueryValue]

  // TODO: document!!!
  protected def evaluateQuery(formula: Query)(current: BindToSensors, lastState: Boolean): QueryValue

  // TODO: document!!!
  protected def makeDecision(query: Query, value: QueryValue, result: Boolean): ClassifiedExercise

  /**
   * Configurable flow defined by implementing subclasses
   */
  protected def workflow: Flow[SensorNetValue, BindToSensors]

  /**
   * Flow that defines how query evaluation influences decision making
   */
  private def evaluate(query: Query) = {
    require(watch.contains(query))

    Flow[List[BindToSensors]].map {
      case _ if stableQuery.contains(query) =>
        stableQuery(query)

      case List(event) =>
        evaluateQuery(watch(query))(event, lastState = true)

      case List(event, _) =>
        evaluateQuery(watch(query))(event, lastState = false)
    }.mapAsync {
      case UnstableValue(nextQuery) =>
        async {
          val nextState = await(simplify(nextQuery))
          watch += (query -> nextState)
          val result = await(satisfiable(nextQuery))

          makeDecision(query, UnstableValue(nextState), result)
        }

      case value: StableValue =>
        stableQuery += (query -> value)

        Future(makeDecision(query, value, value.result))
    }
  }

  override def preStart() = {
    // Reset mutable data structures
    watch.clear()
    stableQuery.clear()
    for (query <- toWatch) {
      watch += (query -> query)
    }

    // Setup model evaluation workflow
    if (watch.isEmpty) {
      // No queries to watch, so we ignore all SensorNetValue's
      FlowGraph { implicit builder =>
        Source(ActorPublisher(self)) ~> Sink.ignore
      }.run()
    } else if (watch.size == 1) {
      FlowGraph { implicit builder =>
        // We have at least one SensorNetValue to watch and report upon
        val split = Unzip[SensorNetValue, ActorRef]
        val join = Zip[ClassifiedExercise, ActorRef]
        val query = watch.keys.head

        Source(ActorPublisher(self)) ~> Flow[(SensorNetValue, ActorRef)].buffer(bufferSize, OverflowStrategy.dropHead) ~> split.in
        // 2 element sliding window allows workflow to look ahead one step and determine if we're in the last state or not
        split.left ~> workflow.transform(() => SlidingWindow[BindToSensors](2)) ~> evaluate(query) ~> join.left
        split.right ~> join.right
        join.out ~> Sink.foreach[(ClassifiedExercise, ActorRef)] { case (exercise, ref) => ref ! exercise}
      }.run()
    } else {
      FlowGraph { implicit builder =>
        val split = Unzip[SensorNetValue, ActorRef]
        val listener = Broadcast[ActorRef]
        val event = Broadcast[List[BindToSensors]]

        Source(ActorPublisher(self)) ~> Flow[(SensorNetValue, ActorRef)].buffer(bufferSize, OverflowStrategy.dropHead) ~> split.in
        // 2 element sliding window allows workflow to look ahead one step and determine if we're in the last state or not
        split.left ~> workflow.transform(() => SlidingWindow[BindToSensors](2)) ~> event
        split.right ~> listener
        for (query <- watch.keys) {
          val join = Zip[ClassifiedExercise, ActorRef]

          event ~> evaluate(query) ~> join.left
          listener ~> join.right
          join.out ~> Sink.foreach[(ClassifiedExercise, ActorRef)] { case (exercise, ref) => ref ! exercise}
        }
      }.run()
    }
  }

  def receive = LoggingReceive {
    // TODO: refactor code so that the following assumptions may be weakened further!
    case event: SensorNet =>
      require(
        event.toMap.values.forall(_.values.nonEmpty),
        "all sensors in a network should produce some sensor value"
      )
      val blockSize = event.toMap.values.head.values.length
      require(
        event.toMap.values.forall(_.values.length == blockSize),
        "all sensors in a network produce the same number of sensor values"
      )
      require(
        event.toMap.values.forall(_.samplingRate == samplingRate),
        "all sensors have a fixed known sample rate"
      )

      val sensorEvents = (0 until blockSize).map(block => SensorNetValue(event.toMap.mapValues(_.values(block))))

      for (evt <- sensorEvents) {
        self.tell(evt, sender())
      }

    case event: SensorNetValue =>
      if (isActive && totalDemand > 0) {
        onNext((event, sender()))
      } else if (isActive) {
        log.error(s"Actor publisher is inactive and we received a SensorNet event, so dropping $event")
      } else {
        log.warning(s"No demand for the actor publisher and we received a SensorNet event, so dropping $event")
      }
  }

}
