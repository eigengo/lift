package com.eigengo.lift.common

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.Cluster
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.typesafe.config.ConfigFactory
import net.nikore.etcd.EtcdClient
import net.nikore.etcd.EtcdExceptions.KeyNotFoundException
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import spray.can.Http
import spray.routing.{HttpServiceActor, Route}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MicroserviceApp {

  class Api(route: Route) extends HttpServiceActor {
    override def receive: Receive = runRoute(route)
  }

  trait BootedNode {
    def api: Option[ExecutionContext ⇒ Route] = None
  }

}

abstract class MicroserviceApp(microserviceName: String)(f: ActorSystem ⇒ BootedNode) extends App {

  object EtcdKeys {
    val ClusterNodes = "akka.cluster.nodes"
  }

  import com.eigengo.lift.common.MicroserviceApp._
  private val name = "Lift"
  private val log = Logger(getClass)

  // HACK: Wait for Cassandra startup.
  Thread.sleep(10000)

  def startup(): Unit = {
    val hostname = InetAddress.getLocalHost.getHostName
    log.info(s"Starting up microservice $microserviceName at $hostname")

    import scala.concurrent.duration._
    val config = ConfigFactory.load()
    val etcd = new EtcdClient(config.getString("etcd.url"))
    log.info(s"Config loaded; etcd expected at $etcd")

    val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds

    log.info("Creating the microservice's ActorSystem")
    // Create an Akka system
    val system = ActorSystem(name, config)
    val cluster = Cluster(system)

    joinCluster()

    def startupApi(api: ExecutionContext ⇒ Route): Unit = {
      val route: Route = api(system.dispatcher)
      val restService = system.actorOf(Props(classOf[Api], route))
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = 8080)
    }

    def run(): Unit = {
      // register the fact that we've joined
      etcd.setKey(s"${EtcdKeys.ClusterNodes}/$hostname", cluster.selfAddress.toString)
      // register shutdown callback
      system.registerOnTermination(shutdown)
      // boot the microservice code
      val bootedNode = f(system)
      bootedNode.api.foreach(startupApi)
      // logme!
      log.info(s"Node ${cluster.selfAddress} up")
    }

    def joinCluster(): Unit = {
      import system.dispatcher
      log.info("Joining the cluster")
      etcd.listDir(EtcdKeys.ClusterNodes, false).onComplete {
        case Success(response: EtcdListResponse) ⇒
          log.debug(s"Using etcd response: $response")
          response.node.nodes match {
            case Some(seedNodes) if seedNodes.size > 1 ⇒
              // At least one seed node has been retrieved from etcd
              val nodes = seedNodes.flatMap(_.value.map(AddressFromURIString.apply)).take(2)
              log.info(s"Seeding cluster using: $nodes")
              // join the nodes
              cluster.joinSeedNodes(nodes)
              // run
              run()

            case Some(_) ⇒
              log.error(s"Failed to retrieve any viable seed nodes - retrying in $retry seconds")
              system.scheduler.scheduleOnce(retry)(joinCluster())

            case None ⇒
              log.error(s"Failed to retrieve any keys for directory ${EtcdKeys.ClusterNodes} - retrying in $retry seconds")
              system.scheduler.scheduleOnce(retry)(joinCluster())
          }

        case Failure(_: KeyNotFoundException) ⇒
          log.info(s"Node ${cluster.selfAddress} is the first node in the cluster")
          run()

        case Failure(ex) ⇒
          log.error(s"$ex")
          shutdown()
      }
    }

    def shutdown(): Unit = {
      // We first ensure that we de-register and leave the cluster!
      etcd.deleteKey(s"${EtcdKeys.ClusterNodes}/$hostname")
      cluster.leave(cluster.selfAddress)
      system.shutdown()
    }
  }


  startup()
}
