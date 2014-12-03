package com.eigengo.lift.common

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.Cluster
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp.{MicroserviceProps, BootedNode}
import com.typesafe.config.ConfigFactory
import net.nikore.etcd.EtcdClient
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import spray.can.Http
import spray.routing.{HttpServiceActor, Route}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Companion for the microservice app
 */
object MicroserviceApp {

  /**
   * The microservice props
   * @param name the microservice name
   */
  case class MicroserviceProps(name: String) {
    val role = name
  }

  /**
   * The REST API actor
   * @param route the route to handle
   */
  private class RestAPIActor(route: Route) extends HttpServiceActor {
    override def receive: Receive = runRoute(route)
  }

  /**
   * Booted node that defines the rest API
   */
  trait BootedNode {
    def api: Option[ExecutionContext ⇒ Route] = None
  }

}

/**
 * All microservice implementations should extend this class, providing the microservice name,
 * @param microserviceProps the microservice properties
 * @param boot the boot function
 */
abstract class MicroserviceApp(microserviceProps: MicroserviceProps)(boot: ActorSystem ⇒ BootedNode) extends App {

  private object EtcdKeys {

    object ClusterNodes {
      val All = "system/frontend.cluster.nodes"
      val Joining = "Joining"

      def apply(cluster: Cluster): String = {
        s"$All/${cluster.selfAddress.host.getOrElse("")}:${cluster.selfAddress.port.getOrElse(0)}"
      }

    }
  }

  import com.eigengo.lift.common.MicroserviceApp._

  private val name = "Lift"
  private val log = Logger(getClass)

  def startup(): Unit = {
    // resolve the local host name
    val hostname = InetAddress.getLocalHost.getHostAddress
    log.info(s"Starting Up microservice $microserviceProps at $hostname")

    // load config and set Up etcd client
    import scala.concurrent.duration._
    val config = ConfigFactory.load()
    val etcd = new EtcdClient(config.getString("etcd.url"))
    log.info(s"Config loaded; etcd expected at $etcd")

    // retry timeout for the cluster formation
    val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds
    val minNrMembers = config.getInt("akka.cluster.min-nr-of-members")

    // Create the ActorSystem for the microservice
    log.info("Creating the microservice's ActorSystem")
    val system = ActorSystem(name, config)
    val cluster = Cluster(system)

    import system.dispatcher

    // register shutdown callback
    system.registerOnTermination(shutdown())

    // register this (cluster) actor system with etcd
    etcd.setKey(EtcdKeys.ClusterNodes(cluster), cluster.selfAddress.toString).onComplete {
      case Success(_) =>
        // Register cluster MemberUp callback
        cluster.registerOnMemberUp {
          log.info(s"Node ${cluster.selfAddress} booting Up")
          // boot the microservice code
          val bootedNode = boot(system)
          bootedNode.api.foreach(startupApi)
          // logme!
          log.info(s"Node ${cluster.selfAddress} Up")
        }
        joinCluster()

      case Failure(exn) =>
        log.error(s"Failed to set state to 'Joining' with etcd: $exn")
        shutdown()
    }

    def startupApi(api: ExecutionContext ⇒ Route): Unit = {
      val route: Route = api(system.dispatcher)
      val restService = system.actorOf(Props(classOf[RestAPIActor], route))
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = 8080)
    }

    def joinCluster(): Unit = {
      log.info("Joining the cluster")

      // Fetch, from etcd, cluster nodes for seeding
      etcd.listDir(EtcdKeys.ClusterNodes.All, recursive = false).onComplete {
        case Success(response: EtcdListResponse) ⇒
          response.node.nodes match {
            // Have any actor systems registered and recorded themselves as Up?
            case Some(systemNodes) ⇒
              log.info(s"Found $systemNodes")
              // At least one actor system address has been retrieved from etcd
              // We now need to check their respective etcd states and locate Up cluster seed nodes
              val seedNodes = systemNodes.flatMap(_.value).map(AddressFromURIString.apply).take(minNrMembers)

              if (seedNodes.size >= minNrMembers) {
                log.info(s"Joining our cluster using the seed nodes: $seedNodes")
                cluster.joinSeedNodes(seedNodes)
              } else {
                log.warning(s"Not enough seed nodes found. Retrying in $retry")
                system.scheduler.scheduleOnce(retry)(joinCluster())
              }

            case None ⇒
              log.warning(s"Failed to retrieve any keys for directory ${EtcdKeys.ClusterNodes.All} - retrying in $retry seconds")
              system.scheduler.scheduleOnce(retry)(joinCluster())
          }

        case Failure(ex) ⇒
          log.error(s"$ex")
          shutdown()
      }
    }

    def shutdown(): Unit = {
      // We first ensure that we de-register and leave the cluster!
      etcd.deleteKey(EtcdKeys.ClusterNodes(cluster))
      cluster.leave(cluster.selfAddress)
      log.info(s"Shut down ActorSystem ${system}")
      system.shutdown()
    }
  }

  startup()
}
