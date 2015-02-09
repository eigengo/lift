package com.eigengo.lift.exercise

object UserExercises {

  /**
   * Model version and other metadata
   * @param version the model version
   */
  case class ModelMetadata(version: Int)

  /**
   * The MD companion
   */
  object ModelMetadata {
    /** Special user-classified metadata */
    val user = ModelMetadata(-1231344)
  }

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
  case class ClassifyExerciseEvt(sessionProps: SessionProperties, sensorData: List[SensorDataWithLocation])

  /**
   * The session has started
   * @param sessionId the session identity
   * @param sessionProps the session props
   */
  case class SessionStartedEvt(sessionId: SessionId, sessionProps: SessionProperties)

  /**
   * The session has been abandoned. Typically, the mobile application has detected a loss of
   * network connectivity or the processor has detected serious gaps in the data stream
   *
   * @param sessionId the session being abandoned
   */
  case class SessionAbandonedEvt(sessionId: SessionId)

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

  /**
   * Set metric on all un-metriced exercises in the current set
   * @param sessionId the session id
   * @param metric the metric to be set
   */
  case class ExerciseSetExerciseMetricEvt(sessionId: SessionId, metric: Metric)
  
}
