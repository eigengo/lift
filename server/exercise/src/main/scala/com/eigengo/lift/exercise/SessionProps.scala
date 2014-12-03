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
                   intendedIntensity: ExerciseIntensity)

