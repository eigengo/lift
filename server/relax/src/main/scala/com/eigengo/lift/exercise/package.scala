package com.eigengo.lift

package object exercise {
  /** The exercise */
  type ExerciseName = String
  /** The exercise intensity 0..1 */
  type ExerciseIntensity = Double

  /** Muscle group */
  type MuscleGroupKey = String

  /**
   * Adds much greater than and much less than operators to ``ExerciseIntensity`` instances
   * @param intensity the wrapped intensity
   */
  implicit class ExerciseIntensityOps(intensity: ExerciseIntensity) {
    private val factor = 0.33

    /**
     * Much greater than operator
     * @param that the intensity to compare
     * @return true if "this" is much greater than "that"
     */
    def >>(that: ExerciseIntensity): Boolean = intensity > that + (that * factor)

    /**
     * Much smaller than operator
     * @param that the intensity to compare
     * @return true if "this" is much smaller than "that"
     */
    def <<(that: ExerciseIntensity): Boolean = intensity < that - (that * factor)
  }

  /**
   * Unit of measure
   */
  sealed trait MetricUnit

  /**
   * All mass units of measure
   */
  object Mass {
    /// kg
    case object Kilogram extends MetricUnit
    // pound
  }

  /**
   * All distance units of measure
   */
  object Distance {
    /// km
    case object Kilometre extends MetricUnit

    // but can also include strange units like case object PoolLength
  }

  /**
   * Metric for the exercise
   * @param value the value
   * @param metricUnit the unit
   */
  case class Metric(value: Double, metricUnit: MetricUnit)

  /**
   * A single recorded exercise
   *
   * @param name the name
   * @param intensity the intensity, if known
   * @param metric the metric
   */
  case class Exercise(name: ExerciseName, intensity: Option[Double], metric: Option[Metric])
}
