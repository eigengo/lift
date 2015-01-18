package com.eigengo.lift.exercise.classifiers.svm

import breeze.linalg.DenseVector
import com.eigengo.lift.exercise.classifiers.svm.SVMClassifier.SVMScale
import com.typesafe.config.ConfigFactory
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen._
import org.scalatest._
import org.scalatest.prop._
import scala.io.Source
import scala.util.Success
import scalaz.DisjunctionFunctions

class SVMModelParserTest extends PropSpec with PropertyChecks with Matchers with DisjunctionFunctions {

  val modelPath = "/models"
  val modelName = "svm-model-tap.features"

  property("able to successfully parse real-world libsvm R files") {
    val fileUrl = getClass.getResource(s"$modelPath/$modelName.libsvm")
    new LibSVMParser(Source.fromURL(fileUrl, "UTF-8").mkString).parse.run().isSuccess
  }

  property("able to successfully parse real-world scale R files") {
    val fileUrl = getClass.getResource(s"$modelPath/$modelName.scale")
    new LibSVMParser(Source.fromURL(fileUrl, "UTF-8").mkString).parse.run().isSuccess
  }

  property("able to successfully parse real-world libsvm and scale R files") {
    val gesture = "tap"
    val fileParser = new SVMModelParser(gesture)(ConfigFactory.parseString(
      s"""
        |classification.model.path = "$modelPath"
        |classification.gesture.$gesture.model = "$modelName"
      """.stripMargin))

    fileParser.model.bimap(_ => false, _.scaled.isDefined)
  }

  property("able to parse randomly generated libsvm key-value samples") {
    forAll(listOf(arbitrary[(String, String)])) { (kv: Seq[(String, String)]) =>
      val testData = kv.map { case (k, v) => s"$k $v" }.mkString("\n")
      val kvParser = new LibSVMParser(testData)

      kvParser.HeaderRule.run() match {
        case Success(`kv`) =>
          assert(true)

        case _ =>
          fail()
      }
    }
  }

  property("able to parse randomly generated libsvm support vector samples") {
    forAll(arbitrary[Double], listOf(arbitrary[(Int, Double)])) { (label: Double, data: Seq[(Int, Double)]) =>
      val testData = s"$label ${data.map { case (index, support) => s"$index:$support" }.mkString(" ")}"
      val indexMax = data.map(_._1).max
      val expectedResult = (label, (0 to indexMax).map(i => (i, data.find(_._1 == i).getOrElse(0))))
      val svParser = new LibSVMParser(testData)

      svParser.SupportVectorIndexRule.run() match {
        case Success(`expectedResult`) =>
          assert(true)

        case _ =>
          fail()
      }
    }
  }

  property("able to parse randomly generated scale samples") {
    forAll(listOf(arbitrary[(Double, Double)])) { (data: Seq[(Double, Double)]) =>
      val testData = data.map { case (x, center) => s"$x $center" }.mkString("\n")
      val expectedResult = SVMScale(
        scale  = DenseVector(data.map(_._1): _*),
        center = DenseVector(data.map(_._2): _*)
      )
      val scaleParser = new SVMScaleParser(testData)

      scaleParser.parse.run() match {
        case Success(`expectedResult`) =>
          assert(true)

        case _ =>
          fail()
      }
    }
  }

}
