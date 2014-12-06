package com.eigengo.lift.exercise

import java.util.{Date, UUID}

/**
 * The session identity
 * @param id the id
 */
case class SessionId(id: UUID) extends AnyVal {
  override def toString = id.toString
}
object SessionId {
  def apply(s: String): SessionId = SessionId(UUID.fromString(s))
  def randomId(): SessionId = SessionId(UUID.randomUUID())
}


/**
 * The exercise session props
 * @param startDate the start date
 * @param muscleGroupKeys the planned muscle groups
 * @param intendedIntensity the planned intensity
 */
case class SessionProps(startDate: Date,
                   muscleGroupKeys: Seq[MuscleGroupKey],
                   intendedIntensity: ExerciseIntensity) {
  require(intendedIntensity >  0.0, "intendedIntensity must be between <0, 1)")
  require(intendedIntensity <= 1.0, "intendedIntensity must be between <0, 1)")

  import scala.concurrent.duration._

  /** At brutal (> .95) intensity, we rest for 15-ish seconds */
  private val brutalRest = 15

  /**
   * The duration between sets
   */
  lazy val restDuration: FiniteDuration = (1.0 / intendedIntensity * brutalRest).seconds


}

