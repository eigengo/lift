package com.eigengo.lift.exercise.classifiers.model.provers

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise.classifiers.model.SMTInterface
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions.{False, True}
import com.typesafe.config.Config
import edu.nyu.acsys.CVC4._
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * CVC4 Install Instructions (for OS X with brew 'support'):
 *   - wget http://cvc4.cs.nyu.edu/builds/src/cvc4-1.4.tar.gz
 *   - tar -xzvf cvc4-1.4.tar.gz
 *   - cd cvc4-1.4
 *   - brew install cln gmp boost libantlr3c swig
 *   - export CPPFLAGS="-I/usr/local/include"
 *   - export LDFLAGS="-L/usr/local/lib"
 *   - ./configure --prefix=/usr/local/Cellar/cvc4/1.4 --enable-language-bindings=java JAVA_CPPFLAGS=-I/System/Library/Frameworks/JavaVM.framework/Headers && make && make install && brew link cvc4
 */
class CVC4(config: Config) extends SMTInterface {

  System.load(config.getString("prover.cvc.library"))

  import ExerciseModel._

  private val em = new ExprManager()
  private val smt = new SmtEngine(em)
  private var queryCount: Int = 0
  private val queryMapping = mutable.HashMap.empty[Query, Expr]
  private var propCount: Int = 0
  private val propMapping = mutable.HashMap.empty[Proposition, Expr]

  // Quantifier-free logic of undefined functions
  smt.setLogic("QF_UF")

  private def propositionToExpr(fact: Proposition): Expr = fact match {
    case Assert(True, _) =>
      em.mkConst(true)

    case Assert(False, _) =>
      em.mkConst(false)

    case prop: Assert =>
      if (!propMapping.contains(prop)) {
        // Ensure both +ve and -ve forms of sensor assertion are represented by propositions
        val newProp = s"sensor_$propCount"
        propCount += 1
        // CVC4 distinguishes variables by their memory instance, so we store freshly generated propositions for latter usage
        propMapping += (prop -> em.mkVar(newProp, em.booleanType()), not(prop) -> em.mkVar(s"not_$newProp", em.booleanType()))
        // "$newProp" != "not_$newProp"
        val eq = em.mkExpr(Kind.DISTINCT, propMapping(prop), propMapping(not(prop)))
        smt.assertFormula(eq) // add relational fact to SMT theory base
      }
      propMapping(prop)

    case Conjunction(fact1, fact2, remaining @ _*) =>
      val and = new vectorExpr()
      for (subFact <- fact1 +: fact2 +: remaining) {
        and.add(propositionToExpr(subFact))
      }
      em.mkExpr(Kind.AND, and)

    case Disjunction(fact1, fact2, remaining @ _*) =>
      val or = new vectorExpr()
      for (subFact <- fact1 +: fact2 +: remaining) {
        or.add(propositionToExpr(subFact))
      }
      em.mkExpr(Kind.OR, or)
  }

  private def queryToExpr(query: Query): Expr = query match {
    case Formula(fact) =>
      propositionToExpr(fact)

    case TT =>
      em.mkConst(true)

    case FF =>
      em.mkConst(false)

    case And(query1, query2, remaining @ _*) =>
      val and = new vectorExpr()
      for (subQuery <- query1 +: query2 +: remaining) {
        and.add(queryToExpr(subQuery))
      }
      em.mkExpr(Kind.AND, and)

    case Or(query1, query2, remaining @ _*) =>
      val or = new vectorExpr()
      for (subQuery <- query1 +: query2 +: remaining) {
        or.add(queryToExpr(subQuery))
      }
      em.mkExpr(Kind.OR, or)

    case _ =>
      // We are dealing with a path quantified LDL formula
      if (!queryMapping.contains(query)) {
        // Ensure both +ve and -ve forms of LDL formula are represented by propositions
        val newProp = s"query_$queryCount"
        queryCount += 1
        // CVC4 distinguishes variables by their memory instance, so we store freshly generated propositions for latter usage
        queryMapping += (query -> em.mkVar(newProp, em.booleanType()), not(query) -> em.mkVar(s"not_$newProp", em.booleanType()))
        // "$newProp" != "not_$newProp"
        val eq = em.mkExpr(Kind.DISTINCT, queryMapping(query), queryMapping(not(query)))
        smt.assertFormula(eq) // add relational fact to SMT theory base
      }
      queryMapping(query)
  }

  def simplify(query: Query)(implicit ec: ExecutionContext): Future[Query] = {
    smt.simplify(queryToExpr(query)) // TODO: implement the map back into an LDL query

    Future.successful(query)
  }

  def satisfiable(query: Query)(implicit ec: ExecutionContext): Future[Boolean] = {
    // Determine if current model state is satisfiable or not
    smt.checkSat(queryToExpr(query)).isSat match {
      case Result.Sat.SAT =>
        Future.successful(true)

      case Result.Sat.UNSAT =>
        Future.successful(false)

      case Result.Sat.SAT_UNKNOWN =>
        Future.failed(new RuntimeException(s"Failed to determine if $query was satisfiable or not"))
    }
  }

  def valid(query: Query)(implicit ec: ExecutionContext): Future[Boolean] = {
    // Determine if current model state is valid or not
    smt.query(queryToExpr(query)).isValid match {
      case Result.Validity.VALID =>
        Future.successful(true)

      case Result.Validity.INVALID =>
        Future.successful(false)

      case Result.Validity.VALIDITY_UNKNOWN =>
        Future.failed(new RuntimeException(s"Failed to determine if $query was valid or not"))
    }
  }

  def statistics: Map[String, String] = {
    smt.getStatistics.iterator().map(stat => (stat(0).toString, stat(1).toString)).toMap
  }

}
