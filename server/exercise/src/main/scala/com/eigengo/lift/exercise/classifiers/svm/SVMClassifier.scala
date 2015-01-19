package com.eigengo.lift.exercise.classifiers.svm

import breeze.linalg._
import breeze.numerics._
import breeze.numerics.constants.Pi

object SVMClassifier {

  case class SVMScale private[svm] (center: DenseVector[Double], scale: DenseVector[Double])
  case class SVMModel private[svm] (nSV: Int, SV: DenseMatrix[Double], gamma: Double, coefs: DenseVector[Double], rho: Double, probA: Double, probB: Double, scaled: Option[SVMScale])
  case class SVMClassification private[svm] (result: Double, negativeMatch: Double, positiveMatch: Double)

}

trait SVMClassifier {

  import SVMClassifier._

  /**
   * 1-dimensional type II discrete cosine transform (DCT)
   *
   * @param data vector of data to be transformed
   * @return     vector of DCT transform coefficients
   */
  private[svm] def discrete_cosine_transform(data: DenseVector[Double]): DenseVector[Double] = {
    val n = dim(data)

    DenseVector.tabulate(n) { (k: Int) => sum(data :* cos(DenseVector.tabulate(n) { (i: Int) => (Pi / n) * (i + 0.5) * k })) }
  }

  /**
   * 2-dimensional type II discrete cosine transform (DCT). Calculated using a row-column algorithm and the 1-dimensional
   * DCT.
   *
   * @param data matrix of data to be transformed
   * @return     concatenation of type I DCT results applied to each data row
   */
  private[svm] def discrete_cosine_transform(data: DenseMatrix[Double]): DenseMatrix[Double] = {
    val rowDCT = DenseMatrix.vertcat((0 until data.rows).map { r => discrete_cosine_transform(data(r,::).t).toDenseMatrix }: _*)

    DenseVector.horzcat((0 until data.cols).map { c => discrete_cosine_transform(rowDCT(::,c)) }: _*)
  }

  /**
   * Radial basis function implementations.
   *   * `radial_kernel`        - standard implementation
   *   * `taylor_radial_kernel` - implementation that uses a taylor expansion (for efficiency)
   */

  protected def radial_kernel(x: DenseVector[Double], y: DenseVector[Double], gamma: Double): Double = {
    exp(-gamma * sum((x :- y) :* (x :- y)))
  }

  protected def taylor_radial_kernel(degree: Int = 2)(x: DenseVector[Double], y: DenseVector[Double], gamma: Double): Double = {
    def factorial(n: Int, accumalator: Int = 1): Int = if (n == 0) { accumalator } else { factorial(n - 1, n * accumalator) }
    val taylor_expansion = (0 until degree).map(i => 1.0 / factorial(i)).toArray[Double]

    exp(-gamma * sum(x :* x)) * exp(-gamma * sum(y :* y)) * polyval(taylor_expansion, gamma * 2 * sum(x :* y))
  }

  /**
   * Use an SVM model to classify given data. Raw classification result and matching probability returned by this function.
   *
   * @param svm  (trained) SVM model to be used in prediction
   * @param data (unseen) data that SVM predictor is to classify
   * @param rbf  radial basis function that the SVM predictor is to use
   */
  def predict(svm: SVMModel, data: DenseMatrix[Double], rbf: (DenseVector[Double], DenseVector[Double], Double) => Double): SVMClassification = {
    val feature = discrete_cosine_transform(data)
    val featureVector = feature.t.toDenseVector // row-major transformation
    val scaled_feature = svm.scaled.map {
      case scaling =>
        (featureVector :- scaling.center) :/ scaling.scale
    }.getOrElse(featureVector)
    val result = sum((0 until svm.nSV).map(j => rbf(svm.SV(j,::).t, scaled_feature, svm.gamma) * svm.coefs(j))) - svm.rho
    val probability = 1 / (1 + exp(svm.probA * result + svm.probB))

    SVMClassification(result, probability, 1-probability)
  }

}
