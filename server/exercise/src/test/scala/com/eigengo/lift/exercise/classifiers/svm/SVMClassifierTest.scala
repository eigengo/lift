package com.eigengo.lift.exercise.classifiers.svm

import breeze.linalg._
import breeze.numerics._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest._
import org.scalatest.prop._

class SVMClassifierTest extends PropSpec with PropertyChecks with Matchers with SVMClassifier {

  import SVMClassifier._

  val accelerometer_data = Seq[(Double, Double, Double)](
    //("x", "y", "z"),
    (8,-24,-1016),
    (-8,-24,-1016),
    (24,-24,-968),
    (24,-24,-1000),
    (40,-24,-1024),
    (8,-16,-1008),
    (8,8,-1000),
    (-8,0,-1072),
    (8,-8,-1016),
    (8,-16,-984),
    (-8,-8,-952),
    (-8,-8,-984),
    (8,-8,-1000),
    (8,-16,-992),
    (16,-16,-1016),
    (24,-32,-1000),
    (32,-32,-984),
    (40,-16,-984),
    (32,-8,-984),
    (24,-16,-992),
    (8,-16,-1008),
    (-8,-16,-960),
    (-8,-24,-976),
    (0,-8,-1008),
    (24,-8,-1008),
    (0,-16,-1000),
    (8,-24,-1016),
    (0,-8,-1000),
    (0,-8,-1008),
    (0,-16,-1096),
    (8,16,-984),
    (8,32,-1024),
    (16,32,-1008),
    (16,16,-1016),
    (16,16,-1048)
  )

  val transformed_accelerometer_data = Seq[(Double, Double, Double)](
    (-35128.00000, 30761.222342, -1.704800e+04),
    (-50.52584,    -78.157142,   2.614284e+02),
    (-156.86142,   209.621971,   -3.188293e+02),
    (-117.48045,   -65.527413,   2.188126e+02),
    (222.48331,    109.825334,   1.181331e+02),
    (-138.84115,   -11.452113,   1.158794e+02),
    (27.43575,     -233.658762,  -1.117183e+01),
    (2.90617,      -174.060692,  -5.663939e+01),
    (-30.13932,    80.229560,    2.589649e+01),
    (-114.03228,   53.264146,    -1.252108e+02),
    (-364.02624,   81.015037,    -1.414627e+02),
    (75.71382,     54.717124,    -1.576452e+02),
    (-82.40380,    29.040620,    -3.942667e+01),
    (145.10678,    -221.869490,  9.641510e+01),
    (-123.55418,   115.059379,   2.418034e+01),
    (81.76091,     41.910091,    2.099437e+01),
    (-30.53840,    46.199262,    4.784329e+01),
    (-149.08325,   198.634993,   -2.829990e+01),
    (-74.70159,    105.543810,   -6.704852e+01),
    (-122.39566,   41.826867,    -3.358281e+01),
    (158.36823,    -44.742911,   -4.479078e+01),
    (12.31073,     1.695736,     4.911191e+01),
    (104.06094,    -80.838996,   5.959887e+01),
    (239.10681,    -94.861755,   7.583907e+01),
    (-111.93775,   109.449679,   -5.152573e+01),
    (196.91188,    -189.505655,  1.051247e+02),
    (-27.09607,    20.909911,    1.391917e+01),
    (21.94124,     -51.201035,   1.492201e+01),
    (-19.55418,    63.097855,    -1.819660e+00),
    (-47.64881,    106.127167,   2.316562e-03),
    (122.39446,    -70.332323,   2.867196e+01),
    (-66.87309,    110.947581,   4.475085e+00),
    (280.51009,    -112.155240,  1.149005e+02),
    (28.19836,     6.521423,     -2.115898e+01),
    (115.13651,    -4.939932,    3.735270e+01)
  )

  def DenseVectorOfNGen(size: Int): Gen[DenseVector[Double]] = for {
    data <- listOfN(size, arbitrary[Double])
  } yield DenseVector(data: _*)

  val DenseVectorGen: Gen[DenseVector[Double]] = for {
    size <- posNum[Int]
    vector <- DenseVectorOfNGen(size)
  } yield vector

  def DenseMatrixOfNGen(rows: Int, cols: Int): Gen[DenseMatrix[Double]] = for {
    data <- listOfN(rows, listOfN(cols, arbitrary[Double]))
  } yield DenseMatrix.tabulate(rows, cols) { case (i, j) => data(i)(j) }

  val DenseMatrixGen: Gen[DenseMatrix[Double]] = for {
    rows <- posNum[Int]
    cols <- posNum[Int]
    matrix <- DenseMatrixOfNGen(rows, cols)
  } yield matrix

  def SVMScaleGen(size: Int): Gen[SVMScale] = for {
    center <- DenseVectorOfNGen(size)
    scale  <- DenseVectorOfNGen(size)
  } yield SVMScale(center, scale)

  def SVMModelOfNGen(rows: Int, cols: Int): Gen[SVMModel] = for {
    sv     <- DenseMatrixOfNGen(rows, rows * cols)
    gamma  <- arbitrary[Double]
    coefs  <- DenseVectorOfNGen(rows)
    rho    <- arbitrary[Double]
    probA  <- arbitrary[Double]
    probB  <- arbitrary[Double]
    scaled <- option(SVMScaleGen(rows * cols))
  } yield SVMModel(rows, sv, gamma, coefs, rho, probA, probB, scaled)

  val SVMModelGen: Gen[SVMModel] = for {
    rows <- posNum[Int]
    cols <- posNum[Int]
    model <- SVMModelOfNGen(rows, cols)
  } yield model

  property("vector and matrix DCTs are equal when matrix is a vector") {
    forAll(DenseVectorGen) { (data: DenseVector[Double]) =>
      assert(discreteCosineTransform(data) === discreteCosineTransform(data.toDenseMatrix).toDenseVector)
    }
  }

  property("accelerometer data has a correct DCT result") {
    val data = DenseMatrix(accelerometer_data: _*)
    val result = DenseMatrix(transformed_accelerometer_data: _*)

    assert(abs(discreteCosineTransform(data) :- result).forall(v => v === (0.0 +- 0.0001)))
  }

  property("standard and taylor approximated RDFs are equal") {
    forAll(Gen.oneOf(2, 3, 4), Gen.oneOf(1 to 5)) { (degree: Int, size: Int) =>
      forAll(DenseVectorOfNGen(size), DenseVectorOfNGen(size), arbitrary[Double]) { (x: DenseVector[Double], y: DenseVector[Double], gamma: Double) =>
        (radialKernel(x, y, gamma) - taylorRadialKernel(degree)(x, y, gamma)) === (0.0 +- 0.03)
      }
    }
  }

  property("SVM model displays consistent prediction with a standard RDF") {
    forAll(Gen.oneOf(1 to 10), Gen.oneOf(1 to 3)) { (rows: Int, cols: Int) =>
      forAll(SVMModelOfNGen(rows, cols)) { (svm: SVMModel) =>
        val dataRows = svm.SV.rows
        val dataCols = svm.SV.cols / svm.SV.rows

        forAll(DenseMatrixOfNGen(dataRows, dataCols)) { (data: DenseMatrix[Double]) =>
          val classification = predict(svm, data, rbf = radialKernel)

          (classification.result < 0) === (classification.negativeMatch > classification.positiveMatch)
          (classification.result >= 0) === (classification.negativeMatch <= classification.positiveMatch)
          (classification.negativeMatch + classification.positiveMatch) === 1
        }
      }
    }
  }

  property("SVM model displays consistent prediction with a taylor approximated RDF") {
    forAll(Gen.oneOf(2, 3, 4), Gen.oneOf(1 to 10), Gen.oneOf(1 to 3)) { (degree: Int, rows: Int, cols: Int) =>
      forAll(SVMModelOfNGen(rows, cols)) { (svm: SVMModel) =>
        val dataRows = svm.SV.rows
        val dataCols = svm.SV.cols / svm.SV.rows

        forAll(DenseMatrixOfNGen(dataRows, dataCols)) { (data: DenseMatrix[Double]) =>
          val classification = predict(svm, data, rbf = taylorRadialKernel(degree))

          (classification.result < 0) === (classification.negativeMatch > classification.positiveMatch)
          (classification.result >= 0) === (classification.negativeMatch <= classification.positiveMatch)
          (classification.negativeMatch + classification.positiveMatch) === 1
        }
      }
    }
  }

}
