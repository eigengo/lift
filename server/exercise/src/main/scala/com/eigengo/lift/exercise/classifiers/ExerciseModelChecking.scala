package com.eigengo.lift.exercise.classifiers

import akka.actor.{ActorRef, Actor}
import com.eigengo.lift.exercise.{SessionProperties, UserExercisesClassifier}

trait ExerciseModelChecking {
  this: Actor =>

  protected var classifier: Option[ActorRef] = None

  def registerModelChecking(sessionProps: SessionProperties): Unit = {
    classifier.foreach(context.stop)
    classifier = Some(context.actorOf(UserExercisesClassifier.props(sessionProps)))
  }
  
  def unregisterModelChecking(): Unit = {
    classifier.foreach(context.stop)
    classifier = None
  }
  
}
