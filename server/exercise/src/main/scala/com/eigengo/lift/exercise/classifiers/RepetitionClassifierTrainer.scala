package com.eigengo.lift.exercise.classifiers

import com.eigengo.lift.exercise.{ExerciseName, MuscleGroupKey}

trait RepetitionClassifierTrainer {
  import breeze.linalg._
  import nak.data._
  import nak.NakContext._
  import nak.liblinear.{SolverType, LiblinearConfig}

  def train(principalAxis: DenseVector[Int], muscleGroupKey: MuscleGroupKey, exerciseName: ExerciseName): Unit = {
    val config = LiblinearConfig(cost = 0.1)
    val featurizer = new Featurizer[DenseVector[Int], String] {
      override def apply(v: DenseVector[Int]): Seq[FeatureObservation[String]] =
        v.map(x ⇒ FeatureObservation(exerciseName)).toScalaVector()
    }
    val example = Example(exerciseName, principalAxis)
    val classifier = trainClassifier(config, featurizer, List(example))

    val res = classifier.predict(principalAxis)
    println(res)
  }

}
