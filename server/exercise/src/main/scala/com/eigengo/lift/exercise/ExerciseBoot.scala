package com.eigengo.lift.exercise

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding
import spray.routing.Route

import scala.concurrent.ExecutionContext

case class ExerciseBoot(userExercises: ActorRef, exerciseDataProcessor: ActorRef, exerciseClassifiers: ActorRef)

/**
 * Starts the actors in this microservice
 */
object ExerciseBoot extends ExerciseService {

  /**
   * Boot the exercise microservice
   * @param system the AS to boot the microservice in
   */
  def boot(notification: ActorRef)(implicit system: ActorSystem): ExerciseBoot = {
    val exerciseClassifiers = system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)
    val userExercise = ClusterSharding(system).start(
      typeName = UserExercises.shardName,
      entryProps = Some(UserExercises.props(notification, exerciseClassifiers)),
      idExtractor = UserExercises.idExtractor,
      shardResolver = UserExercises.shardResolver)
    val exerciseDataProcessor = system.actorOf(ExerciseDataProcessor.props(userExercise), ExerciseDataProcessor.name)

    ExerciseBoot(userExercise, exerciseDataProcessor, exerciseClassifiers)
  }

  /**
   * Starts the route given the exercise boot
   * @param boot the booted exercise microservice
   * @return the route
   */
  def route(boot: ExerciseBoot)(implicit ec: ExecutionContext): Route = exerciseRoute(boot)

}
