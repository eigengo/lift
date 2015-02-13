package com.eigengo.lift.exercise.classifiers.model

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions

trait StandardEvaluation {

  import ClassificationAssertions._
  import ExerciseModel._

  // TODO: introduce memoisation into `evaluate` functions?

  def evaluateAtSensor(path: Proposition, state: BindToSensors): Boolean = path match {
    case Assert(sensor, fact) =>
      state.toMap(sensor).contains(fact)

    case Conjunction(fact1, fact2, remaining @ _*) =>
      val results = (fact1 +: fact2 +: remaining).map(p => evaluateAtSensor(p, state))
      results.forall(_ == true)

    case Disjunction(fact1, fact2, remaining @ _*) =>
      val results = (fact1 +: fact2 +: remaining).map(p => evaluateAtSensor(p, state))
      results.contains(true)
  }

  def emptyEvaluate(query: Query): QueryValue = query match {
    case Formula(_) =>
      StableValue(result = false)

    case TT =>
      StableValue(result = true)

    case FF =>
      StableValue(result = false)

    case And(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => emptyEvaluate(q))
      results.fold(StableValue(result = true))(meet)

    case Or(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => emptyEvaluate(q))
      results.fold(StableValue(result = false))(join)

    case Exists(AssertFact(_), _) =>
      StableValue(result = false)

    case Exists(Test(query1), query2) =>
      meet(emptyEvaluate(query1), emptyEvaluate(query2))

    case Exists(Choice(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(Or(Exists(path1, query1), Exists(path2, query1), remainingPaths.map(p => Exists(p, query1)): _*))

    case Exists(Sequence(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(Exists(path1, Exists(path2, remainingPaths.foldLeft(query1) { case (q, p) => Exists(p, q) })))

    case Exists(Repeat(path), query1) =>
      emptyEvaluate(query1)

    case All(AssertFact(_), _) =>
      StableValue(result = true)

    case All(Test(query1), query2) =>
      join(emptyEvaluate(ExerciseModel.not(query1)), emptyEvaluate(query2))

    case All(Choice(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(And(All(path1, query1), All(path2, query1), remainingPaths.map(p => All(p, query1)): _*))

    case All(Sequence(path1, path2, remainingPaths @ _*), query1) =>
      emptyEvaluate(All(path1, All(path2, remainingPaths.foldLeft(query1) { case (q, p) => All(p, q) })))

    case All(Repeat(path), query1) =>
      emptyEvaluate(query1)
  }

  def evaluateQuery(query: Query)(state: BindToSensors, lastState: Boolean): QueryValue = query match {
    case Formula(fact) =>
      StableValue(result = evaluateAtSensor(fact, state))

    case TT =>
      StableValue(result = true)

    case FF =>
      StableValue(result = false)

    case And(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => evaluateQuery(q)(state, lastState))
      results.fold(StableValue(result = true))(meet)

    case Or(query1, query2, remaining @ _*) =>
      val results = (query1 +: query2 +: remaining).map(q => evaluateQuery(q)(state, lastState))
      results.fold(StableValue(result = false))(join)

    case Exists(AssertFact(fact), query1) if !lastState && evaluateAtSensor(fact, state) =>
      UnstableValue(query1)

    case Exists(AssertFact(fact), query1) if lastState && evaluateAtSensor(fact, state) =>
      emptyEvaluate(query1)

    case Exists(AssertFact(_), _) =>
      StableValue(result = false)

    case Exists(Test(query1), query2) =>
      meet(evaluateQuery(query1)(state, lastState), evaluateQuery(query2)(state, lastState))

    case Exists(Choice(path1, path2, remainingPaths @ _*), query1) =>
      evaluateQuery(Or(Exists(path1, query1), Exists(path2, query1), remainingPaths.map(p => Exists(p, query1)): _*))(state, lastState)

    case Exists(Sequence(path1, path2, remainingPaths @ _*), query1) =>
      evaluateQuery(Exists(path1, Exists(path2, remainingPaths.foldLeft(query1) { case (q, p) => Exists(p, q) })))(state, lastState)

    case Exists(Repeat(path), query1) if testOnly(path) =>
      evaluateQuery(query1)(state, lastState)

    case Exists(Repeat(path), query1) =>
      join(
        evaluateQuery(query1)(state, lastState),
        evaluateQuery(Exists(path, Exists(Repeat(path), query1)))(state, lastState)
      )

    case All(AssertFact(fact), query1) if !lastState && evaluateAtSensor(fact, state) =>
      UnstableValue(query1)

    case All(AssertFact(fact), query1) if lastState && evaluateAtSensor(fact, state) =>
      emptyEvaluate(query1)

    case All(AssertFact(_), _) =>
      StableValue(result = true)

    case All(Test(query1), query2) =>
      join(evaluateQuery(ExerciseModel.not(query1))(state, lastState), evaluateQuery(query2)(state, lastState))

    case All(Choice(path1, path2, remainingPaths @ _*), query1) =>
      evaluateQuery(And(All(path1, query1), All(path2, query1), remainingPaths.map(p => All(p, query1)): _*))(state, lastState)

    case All(Sequence(path1, path2, remainingPaths @ _*), query1) =>
      evaluateQuery(All(path1, All(path2, remainingPaths.foldLeft(query1) { case (q, p) => All(p, q) })))(state, lastState)

    case All(Repeat(path), query1) if testOnly(path) =>
      evaluateQuery(query1)(state, lastState)

    case All(Repeat(path), query1) =>
      meet(
        evaluateQuery(query1)(state, lastState),
        evaluateQuery(All(path, All(Repeat(path), query1)))(state, lastState)
      )
  }

}
