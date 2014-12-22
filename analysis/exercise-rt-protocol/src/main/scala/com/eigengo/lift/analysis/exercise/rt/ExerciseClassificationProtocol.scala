package com.eigengo.lift.analysis.exercise.rt

object ExerciseClassificationProtocol {

  case class Train[A](payload: A, exercise: Exercise)

  case class Classify[A](id: String, payload: A)

}
