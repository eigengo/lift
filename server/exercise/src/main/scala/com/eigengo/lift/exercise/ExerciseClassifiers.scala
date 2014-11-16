package com.eigengo.lift.exercise

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.RoundRobinPool

/**
 * Companion for "all classifiers"
 */
object ExerciseClassifiers {
  val props = Props[ExerciseClassifiers]
  val name = "exercise-classifiers"
}

/**
 * Parent for all internal classifiers
 */
class ExerciseClassifiers extends Actor {

  // we want to maintain a pool of n actors for each model
  private val pool: RoundRobinPool = RoundRobinPool(nrOfInstances = 10)
  context.actorOf(Props(classOf[ExerciseClassifier], NaiveModel).withRouter(pool))
  context.actorOf(Props(classOf[ExerciseClassifier], WaveletModel).withRouter(pool))
  context.actorOf(Props(classOf[ExerciseClassifier], DynamicTimeWrappingModel).withRouter(pool))

  // we replace each child classifier
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable ⇒ Restart
  }

  // this actor handles no messages
  override def receive: Receive = {
    case x ⇒ context.actorSelection("*").tell(x, sender())
  }
}
