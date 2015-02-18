package com.eigengo.lift.exercise.classifiers.model

import com.eigengo.lift.exercise._
import com.eigengo.lift.exercise.classifiers.ExerciseModel._
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait ModelGenerators {

  val defaultDepth = 3

  val SensorQueryGen: Gen[SensorDataSourceLocation] =
    Gen.oneOf(SensorDataSourceLocationWrist, SensorDataSourceLocationWaist, SensorDataSourceLocationFoot, SensorDataSourceLocationChest, SensorDataSourceLocationAny)

  val FactGen: Gen[Fact] = frequency(
    1 -> Gen.oneOf(True, False),
    1 -> (for { name <- arbitrary[String]; matchProbability <- arbitrary[Double] } yield Gesture(name, matchProbability)),
    1 -> (for { name <- arbitrary[String]; matchProbability <- arbitrary[Double] } yield NegGesture(name, matchProbability))
  )

  def PropositionGen(depth: Int = defaultDepth, sensorGen: Option[Gen[SensorDataSourceLocation]] = None): Gen[Proposition] = frequency(
    5 -> (for { sensor <- Gen.lzy(sensorGen.getOrElse(SensorQueryGen)); fact <- Gen.lzy(FactGen) } yield Assert(fact, sensor)),
    1 -> (for { fact1 <- Gen.lzy(PropositionGen(depth-1, sensorGen)); fact2 <- Gen.lzy(PropositionGen(depth-1, sensorGen)) } yield Conjunction(fact1, fact2)),
    1 -> (for { fact1 <- Gen.lzy(PropositionGen(depth-1, sensorGen)); fact2 <- Gen.lzy(PropositionGen(depth-1, sensorGen)) } yield Disjunction(fact1, fact2))
  )

  def PathGen(depth: Int = defaultDepth, sensorGen: Option[Gen[SensorDataSourceLocation]] = None): Gen[Path] = frequency(
    5 -> Gen.lzy(PropositionGen(depth-1, sensorGen)).map(AssertFact),
    5 -> Gen.lzy(QueryGen(depth-1, sensorGen)).map(Test),
    1 -> (for { path1 <- Gen.lzy(PathGen(depth-1, sensorGen)); path2 <- Gen.lzy(PathGen(depth-1, sensorGen)) } yield Choice(path1, path2)),
    1 -> (for { path1 <- Gen.lzy(PathGen(depth-1, sensorGen)); path2 <- Gen.lzy(PathGen(depth-1, sensorGen)) } yield Sequence(path1, path2)),
    5 -> Gen.lzy(PathGen(depth-1, sensorGen)).map(Repeat)
  )

  def QueryGen(depth: Int = defaultDepth, sensorGen: Option[Gen[SensorDataSourceLocation]] = None): Gen[Query] = frequency(
    5 -> (for { sensor <- Gen.lzy(SensorQueryGen); fact <- Gen.lzy(PropositionGen(depth-1, sensorGen)) } yield Formula(fact)),
    5 -> Gen.const(TT),
    5 -> Gen.const(FF),
    1 -> (for { query1 <- Gen.lzy(QueryGen(depth-1, sensorGen)); query2 <- Gen.lzy(QueryGen(depth-1, sensorGen)) } yield And(query1, query2)),
    1 -> (for { query1 <- Gen.lzy(QueryGen(depth-1, sensorGen)); query2 <- Gen.lzy(QueryGen(depth-1, sensorGen)) } yield Or(query1, query2)),
    5 -> (for { path <- Gen.lzy(PathGen(depth-1, sensorGen)); query <- Gen.lzy(QueryGen(depth-1, sensorGen)) } yield Exists(path, query)),
    5 -> (for { path <- Gen.lzy(PathGen(depth-1, sensorGen)); query <- Gen.lzy(QueryGen(depth-1, sensorGen)) } yield All(path, query))
  )

  val QueryValueGen: Gen[QueryValue] = frequency(
    1 -> arbitrary[Boolean].map(StableValue),
    1 -> (for { query <- QueryGen() } yield UnstableValue(query))
  )

}
