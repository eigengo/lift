package com.eigengo.pe

import akka.actor._
import akka.contrib.pattern.ClusterSharding
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.eigengo.pe.exercise._
import com.typesafe.config.ConfigFactory
import spray.routing.HttpServiceActor

class PeMain extends HttpServiceActor with UserExerciseViewService with UserExerciseProcessorService {
  override def receive: Receive = runRoute(userExerciseProcessorRoute ~ userExerciseViewRoute)
}

/**
 * CLI application for the exercise app
 */
object PeMain extends App {

  startup(Seq("2551", "2552", "0"))

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load())

      // Create an Akka system
      implicit val system = ActorSystem("ClusterSystem", config)

      startupSharedJournal(system, startStore = port == "2551", path =
        ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

      val userExerciseRegion = ClusterSharding(system).start(
        typeName = UserExercise.shardName,
        entryProps = Some(UserExercise.props),
        idExtractor = UserExercise.idExtractor,
        shardResolver = UserExercise.shardResolver)

      if (port != "2551" && port != "2552") {
        val exercise = system.actorOf(Props[Exercise], "exercise")

        Thread.sleep(10000)
        // DEMO here
        CliDemo.demo
      }
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
        case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
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
