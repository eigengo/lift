package com.eigengo.lift.exercise.classifiers.model.provers

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise.classifiers.model.ModelGenerators
import org.scalatest._
import org.scalatest.prop._
import scala.concurrent.ExecutionContext.Implicits.global

class CVC4Test
  extends PropSpec
  with PropertyChecks
  with Matchers
  with concurrent.ScalaFutures
  with ModelGenerators {

  import ExerciseModel._

  val cvc4 = new CVC4()

  property("valid(p --> p)") {
    forAll(QueryGen()) { (query: Query) =>
      assert(cvc4.valid(Or(ExerciseModel.not(query), query)).futureValue)
    }
  }

  property("satisfiable(p --> p)") {
    forAll(QueryGen()) { (query: Query) =>
      assert(cvc4.satisfiable(Or(ExerciseModel.not(query), query)).futureValue)
    }
  }

  property("valid(p & q --> p)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(ExerciseModel.not(And(query1, query2)), query1)).futureValue)
    }
  }

  property("satisfiable(p & q --> p)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(ExerciseModel.not(And(query1, query2)), query1)).futureValue)
    }
  }

  property("valid(p & q --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(ExerciseModel.not(And(query1, query2)), query2)).futureValue)
    }
  }

  property("satisfiable(p & q --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(ExerciseModel.not(And(query1, query2)), query2)).futureValue)
    }
  }

  property("valid(p --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(ExerciseModel.not(query1), Or(query1, query2))).futureValue)
    }
  }

  property("satisfiable(p --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(ExerciseModel.not(query1), Or(query1, query2))).futureValue)
    }
  }

  property("valid(q --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(ExerciseModel.not(query2), Or(query1, query2))).futureValue)
    }
  }

  property("satisfiable(q --> p | q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(ExerciseModel.not(query2), Or(query1, query2))).futureValue)
    }
  }

  property("valid(p & (p --> q) --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.valid(Or(ExerciseModel.not(And(query1, Or(ExerciseModel.not(query1), query2))), query2)).futureValue)
    }
  }

  property("satisfiable(p & (p --> q) --> q)") {
    forAll(QueryGen(), QueryGen()) { (query1: Query, query2: Query) =>
      assert(cvc4.satisfiable(Or(ExerciseModel.not(And(query1, Or(ExerciseModel.not(query1), query2))), query2)).futureValue)
    }
  }

}
