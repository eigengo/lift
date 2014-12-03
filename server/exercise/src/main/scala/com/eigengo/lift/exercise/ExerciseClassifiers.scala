package com.eigengo.lift.exercise

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.routing.RoundRobinPool
import com.eigengo.lift.exercise.ExerciseClassifier.Classify
import com.eigengo.lift.exercise.ExerciseClassifiers.{MuscleGroup, GetMuscleGroups}

/**
 * Companion for "all classifiers"
 */
object ExerciseClassifiers {
  val props = Props[ExerciseClassifiers]
  val name = "exercise-classifiers"

  /**
   * Get all supported / classifiable muscle groups. Replies with ``List[MuscleGroup]``
   */
  case object GetMuscleGroups

  /**
   * Muscle group information
   *
   * @param key the key
   * @param title the title
   * @param exercises the suggested exercises
   */
  case class MuscleGroup(key: String, title: String, exercises: List[String])
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

  private val supportedMuscleGroups = List(
    MuscleGroup(key = "legs",  title = "Legs",  exercises = List("squat", "extension", "curl")),
    MuscleGroup(key = "core",  title = "Core",  exercises = List("crunch", "side bend", "cable crunch")),
    MuscleGroup(key = "back",  title = "Back",  exercises = List("pull up", "row", "deadlift", "fly")),
    MuscleGroup(key = "arms",  title = "Arms",  exercises = List("biceps curl", "triceps press down")),
    MuscleGroup(key = "chest", title = "Chest", exercises = List("chest press", "butterfly", "cable cross-over"))
  )

  // we replace each child classifier
  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: Throwable ⇒ Restart
  }

  // this actor handles no messages
  override def receive: Receive = {
    case GetMuscleGroups ⇒ sender() ! supportedMuscleGroups
    case c ⇒ context.actorSelection("*").tell(c, sender())
  }
}
