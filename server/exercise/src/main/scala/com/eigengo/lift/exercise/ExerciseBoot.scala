package com.eigengo.lift.exercise

import akka.actor.{Props, Actor}
import akka.contrib.pattern.ClusterSharding

object ExerciseBoot {
  val props = Props[ExerciseBoot]
}

class ExerciseBoot extends Actor {

  lazy val system = context.system
  val userExercise = ClusterSharding(system).start(
    typeName = UserExercises.shardName,
    entryProps = Some(UserExercises.props),
    idExtractor = UserExercises.idExtractor,
    shardResolver = UserExercises.shardResolver)
  system.actorOf(ExerciseDataProcessor.props(userExercise), ExerciseDataProcessor.name)
  system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)

  override def receive: Receive = Actor.emptyBehavior
}
