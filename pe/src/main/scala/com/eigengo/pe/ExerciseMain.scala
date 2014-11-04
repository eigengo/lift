package com.eigengo.pe

import akka.actor.{ActorSystem, Props}
import com.eigengo.pe.push.UserPushNotification
import org.eigengo.pe.exercise.{UserExerciseProcessor, UserExerciseProtocol, UserExerciseView}
import scodec.bits.BitVector

import scala.io.StdIn

/**
 * CLI application for the exercise app
 */
object ExerciseMain extends App {
  import akka.actor.ActorDSL._
  import com.eigengo.pe.actors._
  import org.eigengo.pe.exercise.UserExerciseProtocol._
  implicit val system = ActorSystem()
  implicit val _ = actor(new Act {
    become {
      case x => println(s">>> $x")
    }
  })
  system.actorOf(Props[UserPushNotification], pushNotification.name)
  val processor = system.actorOf(Props[UserExerciseProcessor])
  val view = system.actorOf(Props[UserExerciseView])

  val is = getClass.getResourceAsStream("/training/chest1.dat")
  val bits = BitVector.fromInputStream(is)
  processor ! ExerciseDataCmd(bits)

  view ! GetExercises

  StdIn.readLine()
  system.shutdown()
}
