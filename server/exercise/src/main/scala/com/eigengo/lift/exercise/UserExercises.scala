package com.eigengo.lift.exercise

import com.eigengo.lift.exercise.UserExerciseClassifier.ModelMetadata
import com.eigengo.lift.exercise.packet.MultiPacket
import scodec.bits.BitVector

object UserExercises {

  /**
   * Failed to decode single packet for the given session with an error message and the original packet
   * @param id the session identity
   * @param error the decoding error
   * @param packet the failing packet
   */
  case class SinglePacketDecodingFailedEvt(id: SessionId, error: String, packet: BitVector)

  /**
   * Failed to decode multi-packet for the given session with an error message and the original packet
   * @param id the session identity
   * @param error the decoding error
   * @param packet the failing packet
   */
  case class MultiPacketDecodingFailedEvt(id: SessionId, error: String, packet: MultiPacket)

  /**
   * Classify the given accelerometer data together with session information
   * @param sessionProps the session
   * @param sensorData the sensor data
   */
  case class ClassifyExerciseEvt(sessionProps: SessionProps, sensorData: List[SensorDataWithLocation])

  /**
   * The session has started
   * @param sessionId the session identity
   * @param sessionProps the session props
   */
  case class SessionStartedEvt(sessionId: SessionId, sessionProps: SessionProps)

  /**
   * The session has been deleted
   * @param sessionId the session that was deleted
   */
  case class SessionDeletedEvt(sessionId: SessionId)

  /**
   * The session has ended
   * @param sessionId the session id
   */
  case class SessionEndedEvt(sessionId: SessionId)

  /**
   * Exercise event received for the given session with model metadata and exercise
   * @param sessionId the session identity
   * @param metadata the model metadata
   * @param exercise the result
   */
  case class ExerciseEvt(sessionId: SessionId, metadata: ModelMetadata, exercise: Exercise)

  /**
   * Explicit (user-provided through tapping the device, for example) of exercise set.
   * @param sessionId the session identity
   */
  case class ExerciseSetExplicitMarkEvt(sessionId: SessionId)

  /**
   * No exercise: rest or just being lazy
   * @param sessionId the session identity
   * @param metadata the model metadata
   */
  case class NoExerciseEvt(sessionId: SessionId, metadata: ModelMetadata)
}
