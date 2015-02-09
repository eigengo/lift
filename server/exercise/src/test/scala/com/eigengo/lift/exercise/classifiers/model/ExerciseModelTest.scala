package com.eigengo.lift.exercise.classifiers.model

import com.eigengo.lift.exercise.classifiers.ExerciseModel
import com.eigengo.lift.exercise.classifiers.workflows.ClassificationAssertions
import com.eigengo.lift.exercise.Sensor
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest._
import org.scalatest.prop._

class ExerciseModelTest extends PropSpec with PropertyChecks with Matchers {

  import ClassificationAssertions._
  import ExerciseModel._

  val defaultDepth = 3

  val SensorQueryGen: Gen[SensorQuery] = frequency(
    1 -> Gen.const(AllSensors),
    1 -> Gen.const(SomeSensor),
    1 -> Gen.oneOf(Sensor.sourceLocations.toList).map(NamedSensor)
  )

  val FactGen: Gen[Fact] = frequency(
    1 -> Gen.oneOf(True, False),
    1 -> (for { name <- arbitrary[String]; matchProbability <- arbitrary[Double] } yield Gesture(name, matchProbability)),
    1 -> (for { name <- arbitrary[String]; matchProbability <- arbitrary[Double] } yield NegGesture(name, matchProbability))
  )

  def PathGen(depth: Int = defaultDepth): Gen[Path] = frequency(
    5 -> (for { sensor <- Gen.lzy(SensorQueryGen); fact <- Gen.lzy(FactGen) } yield Assert(sensor, fact)),
    5 -> Gen.lzy(QueryGen(depth-1)).map(Test),
    1 -> (for { path1 <- Gen.lzy(PathGen(depth-1)); path2 <- Gen.lzy(PathGen(depth-1)) } yield Choice(path1, path2)),
    1 -> (for { path1 <- Gen.lzy(PathGen(depth-1)); path2 <- Gen.lzy(PathGen(depth-1)) } yield Seq(path1, path2)),
    5 -> Gen.lzy(PathGen(depth-1)).map(Repeat)
  )

  def QueryGen(depth: Int = defaultDepth): Gen[Query] = frequency(
    5 -> (for { sensor <- Gen.lzy(SensorQueryGen); fact <- Gen.lzy(FactGen) } yield Formula(sensor, fact)),
    5 -> Gen.const(TT),
    5 -> Gen.const(FF),
    1 -> (for { query1 <- Gen.lzy(QueryGen(depth-1)); query2 <- Gen.lzy(QueryGen(depth-1)) } yield And(query1, query2)),
    1 -> (for { query1 <- Gen.lzy(QueryGen(depth-1)); query2 <- Gen.lzy(QueryGen(depth-1)) } yield Or(query1, query2)),
    5 -> (for { path <- Gen.lzy(PathGen(depth-1)); query <- Gen.lzy(QueryGen(depth-1)) } yield Exists(path, query)),
    5 -> (for { path <- Gen.lzy(PathGen(depth-1)); query <- Gen.lzy(QueryGen(depth-1)) } yield All(path, query))
  )

  val QueryValueGen: Gen[QueryValue] = frequency(
    1 -> arbitrary[Boolean].map(StableValue),
    1 -> (for { result <- arbitrary[Boolean]; query <- QueryGen() } yield UnstableValue(result, query))
  )

  property("meet(complement(x), complement(y)) == complement(join(x, y))") {
    forAll(QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue) =>
      meet(complement(value1), complement(value2)) === complement(join(value1, value2))
    }
  }

  property("complement(complement(x)) == x") {
    forAll(QueryValueGen) { (value: QueryValue) =>
      complement(complement(value)) === value
    }
  }

  property("meet(x, y) == meet(y, x)") {
    forAll(QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue) =>
      meet(value1, value2) === meet(value2, value1)
    }
  }

  property("join(x, y) == join(y, x)") {
    forAll(QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue) =>
      join(value1, value2) === join(value2, value1)
    }
  }

  property("meet(x, meet(y, z)) == meet(meet(x, y), z)") {
    forAll(QueryValueGen, QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue, value3: QueryValue) =>
      meet(value1, meet(value2, value3)) === meet(meet(value1, value2), value3)
    }
  }

  property("join(x, join(y, z)) == join(join(x, y), z)") {
    forAll(QueryValueGen, QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue, value3: QueryValue) =>
      join(value1, join(value2, value3)) === join(join(value1, value2), value3)
    }
  }

  property("join(x, meet(y, z)) == meet(join(x, y), join(x, z))") {
    forAll(QueryValueGen, QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue, value3: QueryValue) =>
      join(value1, meet(value2, value3)) === meet(join(value1, value2), join(value1, value3))
    }
  }

  property("meet(x, join(y, z)) == join(meet(x, y), meet(x, z))") {
    forAll(QueryValueGen, QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue, value3: QueryValue) =>
      meet(value1, join(value2, value3)) === join(meet(value1, value2), meet(value1, value3))
    }
  }

  property("join(x, meet(x, y)) == x") {
    forAll(QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue) =>
      join(value1, meet(value1, value2)) === value1
    }
  }

  property("meet(x, join(x, y)) == x") {
    forAll(QueryValueGen, QueryValueGen) { (value1: QueryValue, value2: QueryValue) =>
      meet(value1, join(value1, value2)) === value1
    }
  }

  property("meet(x, x) == x") {
    forAll(QueryValueGen) { (value: QueryValue) =>
      meet(value, value) === value
    }
  }

  property("join(x, x) == x") {
    forAll(QueryValueGen) { (value: QueryValue) =>
      join(value, value) === value
    }
  }

  property("not(not(x)) == x") {
    forAll(QueryGen()) { (query: Query) =>
      ExerciseModel.not(ExerciseModel.not(query)) === query
    }
  }

}
