package com.eigengo.pe

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.eigengo.pe.exercise.{UserExerciseProcessor, UserExerciseProcessorService, UserExerciseView, UserExerciseViewService}
import com.eigengo.pe.push.UserPushNotification
import spray.can.Http
import spray.routing.HttpServiceActor

import scala.io.StdIn

class PeMain extends HttpServiceActor with UserExerciseViewService with UserExerciseProcessorService {
  override def receive: Receive = runRoute(userExerciseProcessorRoute ~ userExerciseViewRoute)
}

/**
 * CLI application for the exercise app
 */
object PeMain extends App {
  import com.eigengo.pe.actors._

  // Boot up
  implicit val system = ActorSystem()
  system.actorOf(Props[UserPushNotification], pushNotification.name)
  system.actorOf(Props[UserExerciseProcessor], "userExerciseProcessor")
  system.actorOf(Props[UserExerciseView], "userExerciseView")

  // HTTP IO
  val listener = system.actorOf(Props[PeMain])
  IO(Http) ! Http.Bind(listener, interface = "0.0.0.0", port = 8080)

  // To close the system
  StdIn.readLine()
  system.shutdown()
}
