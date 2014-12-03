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
}


/**
 * The exercise session
 * @param id the session identity
 * @param muscleGroupKeys the planned muscle groups
 * @param intendedIntensity the planned intensity
 */
case class Session(id: SessionId, startDate: Date,
                   muscleGroupKeys: Seq[MuscleGroupKey],
                   intendedIntensity: ExerciseIntensity)

