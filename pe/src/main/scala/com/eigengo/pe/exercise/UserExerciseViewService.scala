package com.eigengo.pe.exercise

import spray.routing.HttpService

trait UserExerciseViewService extends HttpService {
  implicit val _ = actorRefFactory.dispatcher
  def userExerciseView = actorRefFactory.actorSelection("/user/userExerciseView")

  import akka.pattern.ask
  import com.eigengo.pe.exercise.UserExerciseView._
  import com.eigengo.pe.exercise.ExerciseClassifier._
  import com.eigengo.pe.timeouts.defaults._


  val userExerciseViewRoute =
    path("exercise") {
      get {
        complete {
          // TODO: Download Json4s dependency. Use proper marshalling
          (userExerciseView ? GetExercises).mapTo[List[ClassifiedExercise]].map(_.toString())
        }
      }
    }

}
