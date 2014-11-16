package com.eigengo.lift.exercise

import akka.actor.ActorSystem
import akka.contrib.pattern.ClusterSharding

/**
 * Starts the actors in this microservice
 */
object ExerciseBoot {

  /**
   * Boot the exercise microservice
   * @param system the AS to boot the microservice in
   */
  def boot(system: ActorSystem): Unit = {
    val userExercise = ClusterSharding(system).start(
      typeName = UserExercises.shardName,
      entryProps = Some(UserExercises.props),
      idExtractor = UserExercises.idExtractor,
      shardResolver = UserExercises.shardResolver)
    system.actorOf(ExerciseDataProcessor.props(userExercise), ExerciseDataProcessor.name)
    system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)

  }
}
