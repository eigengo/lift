package com.eigengo.lift.exercise.classifiers.svm

import breeze.linalg._
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
    (-3.409600e+04, 29874.412329, -1.656800e+04),
    (-1.906482e+01, -90.911871, 2.621685e+02),
    (-1.297973e+02, 200.745908, -3.228594e+02),
    (-1.194419e+02, -61.835447, 1.866880e+02),
    (2.405154e+02, 117.843775, 1.207332e+02),
    (-9.452724e+01, 39.530735, 1.474306e+02),
    (4.942273e+01, -201.394671, 4.433434e+01),
    (5.343040e+01, -233.273025, -1.319334e+01),
    (5.313410e+01, -10.126517, 8.872911e+01),
    (9.793177e+00, -8.217640, -3.866388e+01),
    (-3.547416e+02, 40.178067, -8.741231e+01),
    (-3.331087e-01, 50.281631, -1.719214e+02),
    (-1.457899e+02, 92.441526, -1.356304e+02),
    (1.222872e+02, -246.732642, 1.514395e+01),
    (-1.179198e+02, -13.566545, -5.706216e+00),
    (6.539881e+01, -41.766625, -7.638653e+00),
    (6.090910e+01, -96.083707, 5.658476e+01),
    (-7.919596e+01, 78.383672, 2.828427e+01),
    (-6.852443e+01, 105.392698, -3.191578e+01),
    (-2.638371e+02, 122.456110, -3.242573e+01),
    (-3.593936e+01, 44.229366, -1.038410e+02),
    (-1.367456e+02, 102.064914, -6.388967e+01),
    (-1.558946e+02, 52.232174, -4.493336e+01),
    (1.569776e+02, -96.496690, 5.233252e+01),
    (-1.720807e+02, 184.449211, -1.030064e+02),
    (8.553434e+01, -71.252422, 1.578982e+01),
    (-2.108863e+00, 17.753038, 9.136852e-02),
    (3.042438e+01, -90.197481, 9.437982e+00),
    (2.992513e+01, -89.401397, -7.661078e-01),
    (-1.265986e+02, 78.778078, -2.848192e+01),
    (5.635753e+01, -100.438775, -6.720742e+00),
    (-2.758559e+02, 120.371081, -1.069449e+02),
    (-9.251318e+00, -10.120563, 3.018832e+01),
    (-1.049570e+02, 4.378812, -3.398612e+01)
  )

  val DenseVectorGen: Gen[DenseVector[Double]] = for {
    size <- posNum[Int]
    data <- listOfN(size, arbitrary[Double])
  } yield DenseVector(data: _*)

  val DenseMatrixGen: Gen[DenseMatrix[Double]] = for {
    rows <- posNum[Int]
    cols <- posNum[Int]
    data <- listOfN(rows, listOfN(cols, arbitrary[Double]))
  } yield DenseMatrix.tabulate(rows, cols) { case (i, j) => data(i)(j) }

  val SVMScaleGen: Gen[SVMScale] = for {
    center <- DenseVectorGen
    scale  <- DenseVectorGen
  } yield SVMScale(center, scale)

  val SVMModelGen: Gen[SVMModel] = for {
    nSV    <- posNum[Int]
    sv     <- DenseMatrixGen
    gamma  <- arbitrary[Double]
    coefs  <- DenseVectorGen
    rho    <- arbitrary[Double]
    probA  <- arbitrary[Double]
    probB  <- arbitrary[Double]
    scaled <- option(SVMScaleGen)
  } yield SVMModel(nSV, sv, gamma, coefs, rho, probA, probB, scaled)

  property("vector and matrix DCTs are equal when matrix is a vector") {
    forAll(DenseVectorGen) { (data: DenseVector[Double]) =>
      discrete_cosine_transform(data) === discrete_cosine_transform(data.toDenseMatrix)
    }
  }

  property("accelerometer data has a correct DCT result") {
    val data = DenseMatrix(accelerometer_data: _*)
    val result = DenseMatrix(transformed_accelerometer_data: _*)

    discrete_cosine_transform(data) === result
  }

  property("standard and taylor approximated RDFs are equal") {
    forAll(posNum[Int] suchThat(2 <= _), DenseVectorGen, DenseVectorGen, arbitrary[Double]) { (degree: Int, x: DenseVector[Double], y: DenseVector[Double], gamma: Double) =>
      (radial_kernel(x, y, gamma) - taylor_radial_kernel(degree)(x, y, gamma)) === (0.0 +- 0.03)
    }
  }

  property("SVM model displays consistent prediction with a standard RDF") {
    forAll(SVMModelGen, DenseMatrixGen) { (svm: SVMModel, data: DenseMatrix[Double]) =>
      val classification = predict(svm, data, rbf = radial_kernel)

      (classification.result < 0) === (classification.negativeMatch > classification.positiveMatch)
      (classification.negativeMatch + classification.positiveMatch) === 1
    }
  }

  property("SVM model displays consistent prediction with a taylor approximated RDF") {
    forAll(posNum[Int] suchThat(2 <= _), SVMModelGen, DenseMatrixGen) { (degree: Int, svm: SVMModel, data: DenseMatrix[Double]) =>
      val classification = predict(svm, data, rbf = taylor_radial_kernel(degree))

      (classification.result < 0) === (classification.negativeMatch > classification.positiveMatch)
      (classification.negativeMatch + classification.positiveMatch) === 1
    }
  }

}
