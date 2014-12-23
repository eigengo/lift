package com.eigengo.lift.exercise.classifiers

import com.eigengo.lift.exercise.{ExerciseName, MuscleGroupKey}

trait RepetitionClassifierTrainer {
  import breeze.linalg._
  import nak.data._
  import nak.NakContext._
  import nak.liblinear.{SolverType, LiblinearConfig}

  def train(principalAxis: DenseVector[Int], muscleGroupKey: MuscleGroupKey, exerciseName: ExerciseName): Unit = {
    val npa = normalize(principalAxis.map(_.toDouble))
    val feature = 1
    val example = Example(feature, npa.mapPairs{ case (i, x) ⇒ FeatureObservation(i, x) }.valuesIterator.toList)
    val config = LiblinearConfig(cost = 0.1)
    val model = trainModel(config, List(example), npa.length)

    println(model)
  }

}
