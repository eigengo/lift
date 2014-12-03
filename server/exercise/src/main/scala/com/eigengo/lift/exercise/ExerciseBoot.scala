package com.eigengo.lift.exercise

import akka.actor.{ActorRef, ActorSystem}
import akka.contrib.pattern.ClusterSharding
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.eigengo.lift.exercise.ExerciseBoot._
import com.eigengo.lift.profile.UserProfileLink
import spray.routing.Route
import scala.concurrent.ExecutionContext
import scala.collection.JavaConversions._

case class ExerciseBoot(userExercises: ActorRef, userExercisesView: ActorRef, exerciseClassifiers: ActorRef) extends BootedNode {
  /**
   * Starts the route given the exercise boot
   * @param ec the execution context
   * @return the route
   */
  def route(ec: ExecutionContext): Route = exerciseRoute(userExercises, userExercisesView)(ec)

  override def api: Option[(ExecutionContext) ⇒ Route] = Some(route)
}

/**
 * Starts the actors in this microservice
 */
object ExerciseBoot extends ExerciseService {

  def bootCluster(system: ActorSystem): ExerciseBoot = {
    val profile = UserProfileLink.userProfile(system)
    boot(profile)(system)
  }

  /**
   * Boot the exercise microservice
   * @param system the AS to boot the microservice in
   */
  def boot(profile: ActorRef)(implicit system: ActorSystem): ExerciseBoot = {
    val roles = system.settings.config.getStringList("akka.cluster.roles")
    val exerciseClassifiers = system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)
    val userExercise = ClusterSharding(system).start(
      typeName = UserExercises.shardName,
      entryProps = roles.find("exercise" ==).map(_ => UserExercises.props(profile, exerciseClassifiers)),
      idExtractor = UserExercises.idExtractor,
      shardResolver = UserExercises.shardResolver)
    val userExerciseView = ClusterSharding(system).start(
      typeName = UserExercisesView.shardName,
      entryProps = roles.find("exercise" ==).map(_ => UserExercisesView.props),
      idExtractor = UserExercisesView.idExtractor,
      shardResolver = UserExercisesView.shardResolver)

    ExerciseBoot(userExercise, userExerciseView, exerciseClassifiers)
  }

}
