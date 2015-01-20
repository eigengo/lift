package com.eigengo.lift.exercise.classifiers.svm

import breeze.linalg.DenseVector
import com.eigengo.lift.exercise.classifiers.svm.SVMClassifier.SVMScale
import com.typesafe.config.ConfigFactory
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatest._
import org.scalatest.prop._
import scala.io.Source
import scala.util.{Try, Success, Failure}
import scalaz.DisjunctionFunctions

class SVMModelParserTest extends PropSpec with PropertyChecks with Matchers with DisjunctionFunctions {

  import LibSVMParser._

  val modelPath = "/models"
  val modelName = "svm-model-tap-features"

  def normaliseHeader(hdr: Header): Header = {
    Header(hdr.values.map { case HeaderEntry(k, v) =>
      val normalisedValue =
        Try(v.toInt.toString) orElse
          Try(v.toDouble.toString) orElse
          Success(v)
      assert(normalisedValue.isSuccess)

      HeaderEntry(k, normalisedValue.get)
    })
  }

  val ValueGen: Gen[String] = frequency(
    1 -> Gen.identifier,
    1 -> (Gen.alphaStr suchThat(_.nonEmpty)),
    1 -> arbitrary[Int].map(_.toString),
    1 -> arbitrary[Double].map(_.toString),
    1 -> nonEmptyListOf(arbitrary[Int]).map(_.mkString(" "))
  )

  val KeyValueGen: Gen[(String, String)] = for {
    fst <- Gen.identifier
    snd <- ValueGen
  } yield (fst, snd)

  val SVGen: Gen[(Int, Double)] = for {
    index <- Gen.oneOf(1 to 100)
    value <- arbitrary[Double]
  } yield (index, value)

  property("able to successfully parse real-world libsvm R files") {
    val fileUrl = getClass.getResource(s"$modelPath/$modelName.libsvm")
    assert(new LibSVMParser(Source.fromURL(fileUrl, "UTF-8").mkString).parse.run().isSuccess)
  }

  property("able to successfully parse real-world scale R files") {
    val fileUrl = getClass.getResource(s"$modelPath/$modelName.scale")
    assert(new SVMScaleParser(Source.fromURL(fileUrl, "UTF-8").mkString).parse.run().isSuccess)
  }

  property("able to successfully parse real-world libsvm and scale R files") {
    val gesture = "tap"
    val fileParser = new SVMModelParser(gesture)(ConfigFactory.parseString(
      s"""
        |classification.model.path = "$modelPath"
        |classification.gesture.$gesture.model = "$modelName"
      """.stripMargin))

    assert(fileParser.model.isSuccess && fileParser.model.get.scaled.isDefined)
  }

  property("able to parse randomly generated libsvm key-value samples") {
    forAll(nonEmptyListOf(KeyValueGen)) { (kv: Seq[(String, String)]) =>
      val testData = kv.map { case (k, v) => s"$k $v" }.mkString("\n")
      val expectedResult = Header(kv.map { case (k, v) => HeaderEntry(k, v) })
      val kvParser = new LibSVMParser(testData)
      kvParser.HeaderRule.run() match {
        case Success(actualResult) =>
          assert(normaliseHeader(actualResult) == normaliseHeader(expectedResult))

        case _ =>
          fail()
      }
    }
  }

  property("able to parse randomly generated libsvm support vector samples") {
    forAll(Gen.oneOf(1 to 20)) { (size: Int) =>
      forAll(arbitrary[Double], listOfN(size, SVGen)) { (label: Double, data: Seq[(Int, Double)]) =>
        val testData = s"$label ${data.map { case (index, support) => s"$index:$support"}.mkString(" ")}"
        val indexMax = data.map(_._1).max
        val expectedResult = SupportVectorEntry(label, data.map { case (index, support) => SupportVectorIndex(index, support) })
        val svParser = new LibSVMParser(testData)

        svParser.SupportVectorEntryRule.run() match {
          case Success(`expectedResult`) =>
            assert(true)

          case _ =>
            fail()
        }
      }
    }
  }

  property("able to parse randomly generated scale samples") {
    forAll(listOf(arbitrary[(Double, Double)])) { (data: Seq[(Double, Double)]) =>
      val testData = data.map { case (center, x) => s"$center $x" }.mkString("\n")
      val expectedResult = SVMScale(
        scale  = DenseVector(data.map(_._2): _*),
        center = DenseVector(data.map(_._1): _*)
      )
      val scaleParser = new SVMScaleParser(testData)

      scaleParser.parse.run() match {
        case Success(`expectedResult`) =>
          assert(true)

        case result =>
          fail()
      }
    }
  }

}
