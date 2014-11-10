package com.eigengo.pe

import akka.actor.{ActorSystem, Props}
import akka.contrib.pattern.ClusterSharding
import akka.io.IO
import com.eigengo.pe.exercise._
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
object PeMain extends App with CliDemo {
  import com.eigengo.pe.actors._

  // Boot up
  implicit val system = ActorSystem()
  system.actorOf(Props[UserPushNotification], pushNotification.name)
//  system.actorOf(Props[Exercise], "exercise")
//  system.actorOf(Props[UserExercise], "userExercise")
  system.actorOf(Props[UserExerciseView], "userExerciseView")

  val userExerciseRegion = ClusterSharding(system).start(
    typeName = UserExercise.shardName,
    entryProps = Some(UserExercise.props),
    idExtractor = UserExercise.idExtractor,
    shardResolver = UserExercise.shardResolver)


  // HTTP IO
  val listener = system.actorOf(Props[PeMain])
  IO(Http) ! Http.Bind(listener, interface = "0.0.0.0", port = 8080)

  demo()

  // To close the system
  StdIn.readLine()
  system.shutdown()
}
