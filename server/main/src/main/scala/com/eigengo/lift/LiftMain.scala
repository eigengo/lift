package com.eigengo.lift

import akka.actor._
import akka.contrib.pattern.ClusterSharding
import akka.io.IO
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.eigengo.lift.exercise.{ExerciseClassifiers, ExerciseService, ExerciseDataProcessor, UserExercises}
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.HttpServiceActor

class LiftMain extends HttpServiceActor with ExerciseService {
  override def receive: Receive = runRoute(exerciseRoute)
}

/**
 * CLI application for the exercise app
 */
object LiftMain extends App {
  val LiftActorSystem = "Lift"

  singleJvmStartup(Seq(2551, 2552, 2553, 2554))

  def singleJvmStartup(ports: Seq[Int]): Unit = {
    ports.foreach { port ⇒
      import scala.collection.JavaConverters._
      // Override the configuration of the port
      val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load())
      val firstSeedNodePort = (for {
        seedNode ← config.getStringList("akka.cluster.seed-nodes").asScala
        port ← ActorPath.fromString(seedNode).address.port
      } yield port).head

      // Create an Akka system
      implicit val system = ActorSystem(LiftActorSystem, config)

      // Startup the journal
      startupSharedJournal(system, startStore = port == firstSeedNodePort, path = ActorPath.fromString(s"akka.tcp://$LiftActorSystem@127.0.0.1:$firstSeedNodePort/user/store"))

      // Start the shards
      val userExercise = ClusterSharding(system).start(
         typeName = UserExercises.shardName,
         entryProps = Some(UserExercises.props),
         idExtractor = UserExercises.idExtractor,
         shardResolver = UserExercises.shardResolver)
      system.actorOf(ExerciseDataProcessor.props(userExercise), ExerciseDataProcessor.name)
      system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)

      // ClusterSharding(system).start(
      //   typeName = UserExerciseDataProcessor.shardName,
      //   entryProps = Some(UserExerciseDataProcessor.props(userExercise)),
      //   idExtractor = UserExerciseDataProcessor.idExtractor,
      //   shardResolver = UserExerciseDataProcessor.shardResolver)

      // // Start other actors & views
      // system.actorOf(PushNotification.props, PushNotification.name)
      // system.actorOf(ExerciseClassifiers.props, ExerciseClassifiers.name)

      startupHttpService(system, port)
    }

    def startupHttpService(system: ActorSystem, port: Int): Unit = {
      val restService = system.actorOf(Props[LiftMain])
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = 10000 + port)
    }

    def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
      import akka.pattern.ask
      import scala.concurrent.duration._
      
      // Start the shared journal one one node (don't crash this SPOF)
      // This will not be needed with a distributed journal
      if (startStore) system.actorOf(Props[SharedLeveldbStore], "store")

      // register the shared journal
      import system.dispatcher
      implicit val timeout = Timeout(15.seconds)
      val f = system.actorSelection(path) ? Identify(None)
      f.onSuccess {
        case ActorIdentity(_, Some(ref)) ⇒ SharedLeveldbJournal.setStore(ref, system)
        case _ =>
          system.log.error("Shared journal not started at {}", path)
          system.shutdown()
      }
      f.onFailure {
        case _ =>
          system.log.error("Lookup of shared journal at {} timed out", path)
          system.shutdown()
      }
    }
  }

}
