package com.eigengo.pe.exercise

import java.util.{Date, UUID}

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence.{PersistentView, SnapshotOffer}
import com.eigengo.pe.push.UserPushNotification
import com.eigengo.pe.{AccelerometerData, actors}

object ExerciseView {
  val name = "exercise-view"
  val props = Props[ExerciseView]

  def lookup(implicit arf: ActorRefFactory) = actors.local.lookup(arf, name)

  /**
   * The event with processed fitness data into ``List[AccelerometerData]``
   * @param data the accelerometer data
   */
  case class ExerciseDataEvt(userId: UUID, data: List[AccelerometerData])

}

/**
 * View that handles processing the events, delegates to the classifiers,
 * and provides the query functions.
 */
class ExerciseView extends PersistentView with ActorLogging {
  import com.eigengo.pe.exercise.ExerciseClassifier._
  import com.eigengo.pe.exercise.ExerciseView._

  context.actorOf(Props(classOf[ExerciseClassifier], NaiveModel))
  context.actorOf(Props(classOf[ExerciseClassifier], WaveletModel))
  context.actorOf(Props(classOf[ExerciseClassifier], DynamicTimeWrappingModel))

  log.info("ExerciseView started")

  override val viewId: String = "exercise-view"

  override val persistenceId: String = "exercise-persistence"

  override def receive: Receive = {
    // send the exercise to be classified to the children
    case ExerciseDataEvt(userId, data) if isPersistent ⇒
      log.info(s"ExerciseDataEvt in AS ${self.path.toString}")
      context.actorSelection("*") ! ClassifyExercise(userId, data)

    // classification received
    case ce@ClassifiedExercise(userId, result) =>
      log.info(s"ClassifiedExercise in AS ${self.path.toString}")
      UserExercise.lookup ! ce
      saveSnapshot(new Date())

  }
}
