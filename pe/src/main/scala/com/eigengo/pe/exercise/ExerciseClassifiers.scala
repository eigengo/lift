package com.eigengo.pe.exercise

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.RoundRobinPool
import com.eigengo.pe.actors

/**
 * Companion for "all classifiers"
 */
object ExerciseClassifiers {
  val props = Props[ExerciseClassifiers]
  val name = "exercise-classifiers"

  /**
   * Lookup selection of all available classifiers
   * @param arf the ActorContext or ActorSystem
   * @return the selection of all available ``ExerciseClassifier`` actors
   */
  def lookup(implicit arf: ActorRefFactory) = actors.local.lookup(arf, s"$name/*")
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
  override def receive: Receive = Actor.emptyBehavior
}
