package com.eigengo.pe.exercise

import akka.actor.{ActorRefFactory, Actor, Props}
import com.eigengo.pe.actors

object ExerciseClassifiers {
  val props = Props[ExerciseClassifiers]
  val name = "exercise-classifiers"

  def lookup(implicit arf: ActorRefFactory) = actors.local.lookup(arf, s"$name/*")
}

class ExerciseClassifiers extends Actor {
  context.actorOf(Props(classOf[ExerciseClassifier], NaiveModel))
  context.actorOf(Props(classOf[ExerciseClassifier], WaveletModel))
  context.actorOf(Props(classOf[ExerciseClassifier], DynamicTimeWrappingModel))

  override def receive: Receive = Actor.emptyBehavior
}
