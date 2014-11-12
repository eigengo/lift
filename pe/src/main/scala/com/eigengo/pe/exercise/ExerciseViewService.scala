package com.eigengo.pe.exercise

import java.util.UUID

import akka.actor.ActorRef
import spray.routing.HttpService

trait ExerciseViewService extends HttpService {
  import akka.pattern.ask
  import com.eigengo.pe.exercise.ExerciseView._
  import com.eigengo.pe.timeouts.defaults._
  implicit val _ = actorRefFactory.dispatcher
  def exerciseView: ActorRef = ExerciseView.lookup

  val exerciseViewRoute =
    path("exercise") {
      get {
        complete {
          // TODO: proper marshalling
          (exerciseView ? GetExercises(UUID.fromString("091284FA-2044-435E-BC6B-0E5EE34A6C77"))).map(_.toString)
        }
      }
    }

}
