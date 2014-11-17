package com.eigengo.lift.exercise

import java.util.{Date, UUID}

/**
 * The session identity
 * @param id the id
 */
case class SessionId(id: UUID) extends AnyVal {
  override def toString = id.toString
}

/**
 * Muscle groups enum
 */
sealed trait MuscleGroup
case object Arms extends MuscleGroup
case object Chest extends MuscleGroup
case object Back extends MuscleGroup
case object Legs extends MuscleGroup

/**
 * The exercise session
 * @param id the session identity
 * @param muscleGroups the planned muscle groups
 * @param intendedIntensity the planned intensity
 */
case class Session(id: SessionId, startDate: Date,
                   muscleGroups: Seq[MuscleGroup],
                   intendedIntensity: ExerciseIntensity)

