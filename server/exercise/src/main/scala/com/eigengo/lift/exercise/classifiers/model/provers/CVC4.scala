package com.eigengo.lift.exercise.classifiers.model.provers

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise.classifiers.model.SMTInterface
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions.{False, True}
import edu.nyu.acsys.CVC4._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * CVC4 Install Instructions (for OS X with brew 'support'):
 *   - wget http://cvc4.cs.nyu.edu/builds/src/cvc4-1.4.tar.gz
 *   - tar -xzvf cvc4-1.4.tar.gz
 *   - cd cvc4-1.4
 *   - brew install libantlr3c swig
 *   - ./configure --prefix=/usr/local/Cellar/cvc4/1.4 --enable-language-bindings=java JAVA_CPPFLAGS=-I/System/Library/Frameworks/JavaVM.framework/Headers && make && make install && brew link cvc4
 */
class CVC4 extends SMTInterface {

  System.loadLibrary("cvc4jni")

  import ExerciseModel._

  private val em = new ExprManager()
  private val smt = new SmtEngine(em)
  private var queryCount: Int = 0
  private val queryMapping = mutable.HashMap.empty[Query, String]
  private var propCount: Int = 0
  private val propMapping = mutable.HashMap.empty[Proposition, String]

  // Quantifier-free logic of undefined functions
  smt.setLogic("QF_UF")

  private def mapToProposition(fact: Proposition): Expr = fact match {
    case Assert(_, True) =>
      em.mkConst(true)

    case Assert(_, False) =>
      em.mkConst(false)

    case prop: Assert =>
      if (!propMapping.contains(prop)) {
        // Ensure both +ve and -ve forms of sensor assertion are represented by propositions
        val newProp = s"sensor_$propCount"
        propCount += 1
        propMapping += (prop -> newProp, not(prop) -> s"not_$newProp")
        // "$newProp" == ~ "not_$newProp"
        val eq = new vectorExpr()
        eq.add(em.mkVar(newProp, em.booleanType()))
        eq.add(em.mkExpr(Kind.NOT, em.mkVar(s"not_$newProp", em.booleanType())))
        smt.assertFormula(em.mkExpr(Kind.EQUAL, eq)) // add relational fact to SMT theory base
      }
      em.mkVar(propMapping(prop), em.booleanType())

    case Conjunction(fact1, fact2, remaining @ _*) =>
      val and = new vectorExpr()
      for (subFact <- fact1 +: fact2 +: remaining) {
        and.add(mapToProposition(subFact))
      }
      em.mkExpr(Kind.AND, and)

    case Disjunction(fact1, fact2, remaining @ _*) =>
      val or = new vectorExpr()
      for (subFact <- fact1 +: fact2 +: remaining) {
        or.add(mapToProposition(subFact))
      }
      em.mkExpr(Kind.OR, or)
  }

  private def mapToProposition(query: Query): Expr = query match {
    case Formula(fact) =>
      mapToProposition(fact)

    case TT =>
      em.mkConst(true)

    case FF =>
      em.mkConst(false)

    case And(query1, query2, remaining @ _*) =>
      val and = new vectorExpr()
      for (subQuery <- query1 +: query2 +: remaining) {
        and.add(mapToProposition(subQuery))
      }
      em.mkExpr(Kind.AND, and)

    case Or(query1, query2, remaining @ _*) =>
      val or = new vectorExpr()
      for (subQuery <- query1 +: query2 +: remaining) {
        or.add(mapToProposition(subQuery))
      }
      em.mkExpr(Kind.OR, or)

    case _ =>
      // We are dealing with a path quantified LDL formula
      if (!queryMapping.contains(query)) {
        // Ensure both +ve and -ve forms of LDL formula are represented by propositions
        val newProp = s"query_$queryCount"
        queryCount += 1
        queryMapping += (query -> newProp, not(query) -> s"not_$newProp")
        // "$newProp" == ~ "not_$newProp"
        val eq = new vectorExpr()
        eq.add(em.mkVar(newProp, em.booleanType()))
        eq.add(em.mkExpr(Kind.NOT, em.mkVar(s"not_$newProp", em.booleanType())))
        smt.assertFormula(em.mkExpr(Kind.EQUAL, eq)) // add relational fact to SMT theory base
      }
      em.mkVar(queryMapping(query), em.booleanType())
  }

  // TODO: provide a viable implementation for simplify
  def simplify(query: Query)(implicit ec: ExecutionContext): Future[Query] = Future.successful(query)

  def satisfiable(query: Query)(implicit ec: ExecutionContext): Future[Boolean] = {
    // Determine if current model state is satisfiable or not
    smt.checkSat(mapToProposition(query)).isSat match {
      case Result.Sat.SAT =>
        Future.successful(true)

      case Result.Sat.UNSAT =>
        Future.successful(false)

      case Result.Sat.SAT_UNKNOWN =>
        Future.failed(new RuntimeException(s"Failed to determine if $query was satisfiable or not"))
    }
  }

}
