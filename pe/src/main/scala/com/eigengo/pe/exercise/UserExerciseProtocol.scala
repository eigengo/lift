package com.eigengo.pe.exercise

import com.eigengo.pe.AccelerometerData
import scodec.bits.BitVector

/**
 * Defines the data flowing through the system
 */
object UserExerciseProtocol {

  /**
   * The exercise command with the ``bits`` received from the fitness device
   * @param bits the received data
   */
  case class ExerciseDataCmd(bits: BitVector)

  /**
   * The event with processed fitness data into ``List[AccelerometerData]``
   * @param data the accelerometer data
   */
  case class ExerciseDataEvt(data: List[AccelerometerData])

  /** The exercise */
  type Exercise = String

  /**
   * Classified exercise
   *
   * @param confidence the classification confidence 0..1
   * @param exercise the classified exercise, if any
   */
  case class ClassifiedExercise(confidence: Double, exercise: Option[Exercise])

  /**
   * List all user's exercises
   */
  case object GetExercises

}
