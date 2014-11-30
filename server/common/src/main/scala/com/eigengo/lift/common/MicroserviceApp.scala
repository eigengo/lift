package com.eigengo.lift.common

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.Cluster
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp.BootedNode
import com.typesafe.config.ConfigFactory
import net.nikore.etcd.EtcdClient
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

  def startup(): Unit = {
    val hostname = InetAddress.getLocalHost.getHostAddress
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

    import system.dispatcher

    // register shutdown callback
    system.registerOnTermination(shutdown())

    // register this (cluster) actor system with etcd
    etcd.setKey(s"${EtcdKeys.ClusterNodes}/${clusterAddressKey()}", "Joining").onComplete {
      case Success(_) =>
        // Register cluster MemberUp callback
        cluster.registerOnMemberUp {
          log.info(s"*********** Node ${cluster.selfAddress} booting up")
          etcd.setKey(s"${EtcdKeys.ClusterNodes}/${clusterAddressKey()}", "MemberUp")
          // boot the microservice code
          val bootedNode = f(system)
          bootedNode.api.foreach(startupApi)
          // logme!
          log.info(s"*********** Node ${cluster.selfAddress} up")
        }
        joinCluster()

      case Failure(exn) =>
        log.error(s"Failed to set state to 'Joining' with etcd: $exn")
        shutdown()
    }

    def startupApi(api: ExecutionContext ⇒ Route): Unit = {
      val route: Route = api(system.dispatcher)
      val restService = system.actorOf(Props(classOf[Api], route))
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = 8080) // FIXME: should we use 0.0.0.0 here?
    }

    def joinCluster(): Unit = {
      log.info("Joining the cluster")

      // Fetch, from etcd, cluster nodes for seeding
      etcd.listDir(EtcdKeys.ClusterNodes, recursive = false).onComplete {
        case Success(response: EtcdListResponse) ⇒
          log.debug(s"Using etcd response: $response")
          response.node.nodes match {
            // Have any actor systems registered and recorded themselves as up?
            case Some(systemNodes)
              if systemNodes.filter(_.value == Some("MemberUp")).nonEmpty => {

              // At least one actor system address has been retrieved from etcd - we now need to check their respective etcd states and locate up cluster seed nodes
              val seedNodes =
                systemNodes
                  .filter(_.value == Some("MemberUp"))
                  .map(n => clusterAddress(n.key.stripPrefix(s"/${EtcdKeys.ClusterNodes}/")))

              log.info(s"Joining our cluster using the seed nodes: $seedNodes")
              cluster.joinSeedNodes(seedNodes)
            }

            case Some(_) ⇒
              log.warning(s"Not enough seed nodes found. Retrying in $retry")
              system.scheduler.scheduleOnce(retry)(joinCluster())

            case None ⇒
              log.warning(s"Failed to retrieve any keys for directory ${EtcdKeys.ClusterNodes} - retrying in $retry seconds")
              system.scheduler.scheduleOnce(retry)(joinCluster())
          }

        case Failure(ex) ⇒
          log.error(s"$ex")
          shutdown()
      }
    }

    def clusterAddressKey(): String = {
      s"${cluster.selfAddress.host.getOrElse("")}:${cluster.selfAddress.port.getOrElse(0)}"
    }

    def clusterAddress(key: String): Address = {
      AddressFromURIString(s"akka.tcp://$name@$key")
    }

    def shutdown(): Unit = {
      // We first ensure that we de-register and leave the cluster!
      etcd.deleteKey(s"${EtcdKeys.ClusterNodes}/${clusterAddressKey()}")
      cluster.leave(cluster.selfAddress)
      system.shutdown()
    }
  }


  startup()
}
