package com.eigengo.lift.exercise.classifiers

import java.io.File

import org.scalatest.{Matchers, FlatSpec}

class RepetitionClassifierTrainerTest extends FlatSpec with Matchers with RepetitionClassifierTrainer {

  "RepetitionClassifierTrainer" should "train model" in {
    import breeze.linalg._
    //val m = csvread(new File(getClass.getResource("/training/chest-1-pa.csv").toURI))
    //val v = m.flatten().map(_.toInt)
    //train(v, "chest", "fly")
  }
}
