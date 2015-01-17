package com.eigengo.lift.exercise.classifiers

import breeze.linalg._
import breeze.numerics._
import breeze.numerics.constants.Pi

trait SVMClassifier {

  case class SVMScale(center: DenseVector[Double], scale: DenseVector[Double])
  case class SVMModel(tag: String, nSV: Int, SV: DenseMatrix[Double], gamma: Double, coefs: DenseVector[Double], rho: Double, probA: Double, probB: Double, scaled: SVMScale)
  case class SVMClassification(result: Double, probability: Map[String, Double])

  /**
   * 1-dimensional type II discrete cosine transform (DCT)
   *
   * @param data vector of data to be transformed
   * @return     vector of DCT transform coefficients
   */
  def discrete_cosine_transform(data: DenseVector[Double]): DenseVector[Double] = {
    val n = dim(data)

    DenseVector.tabulate(n) { k => sum(data :* cos(DenseVector.tabulate(n) { i => (Pi / n) * (i + 0.5) * k })) }
  }

  /**
   * 2-dimensional type II discrete cosine transform (DCT). Calculated using a row-column algorithm and the 1-dimensional
   * DCT.
   *
   * @param data matrix of data to be transformed
   * @return     concatenation of type I DCT results applied to each data row
   */
  def discrete_cosine_transform(data: DenseMatrix[Double]): DenseVector[Double] = {
    DenseVector.vertcat((0 to data.rows).map { c => discrete_cosine_transform(data(c,::).t) }: _*)
  }

  def radial_kernel(x: DenseVector[Double], y: DenseVector[Double], gamma: Double): Double = {
    exp(-gamma * sum((x :- y) :* (x :- y)))
  }

  def taylor_radial_kernel(degree: Int = 2)(x: DenseVector[Double], y: DenseVector[Double], gamma: Double): Double = {
    def factorial(n: Int): Int = if (n == 0) { 1 } else { n * factorial(n - 1) }
    val taylor_expansion = (0 to degree).map(i => 1.0 / factorial(i)).toArray[Double]

    exp(-gamma * sum(x :* x)) * exp(-gamma * sum(y :* y)) * polyval(taylor_expansion, gamma * 2 * sum(x :* y))
  }

  def predict(svm: SVMModel, data: DenseMatrix[Double], rbf: (DenseVector[Double], DenseVector[Double], Double) => Double) {
    val feature = discrete_cosine_transform(data)
    val scaled_feature = (feature :- svm.scaled.center) :/ svm.scaled.scale
    val result = sum((0 to svm.nSV).map(j => rbf(svm.SV(j,::).t, scaled_feature, svm.gamma) * svm.coefs(j))) - svm.rho
    val probability = 1 / (1 + exp(svm.probA * result + svm.probB))

    SVMClassification(result, Map("" -> probability, svm.tag -> (1-probability)))
  }

}
