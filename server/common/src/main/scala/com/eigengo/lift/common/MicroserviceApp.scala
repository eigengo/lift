package com.eigengo.lift.common

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterStartup
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp.{MicroserviceProps, BootedNode}
import com.typesafe.config.ConfigFactory
import net.nikore.etcd.EtcdClient
import net.nikore.etcd.EtcdJsonProtocol.EtcdListResponse
import spray.can.Http
import spray.routing.{RouteConcatenation, HttpServiceActor, Route}

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
    import BootedNode._
    def api: Option[RestApi] = None

    def +(that: BootedNode): BootedNode = (this.api, that.api) match {
      case (Some(r1), Some(r2)) ⇒ Default(r1, r2)
      case (Some(r1), None) ⇒ this
      case (None, Some(r2)) ⇒ that
      case _ ⇒ this
    }
  }

  object BootedNode {
    val empty: BootedNode = new BootedNode {}
    type RestApi = ExecutionContext ⇒ Route
    case class Default(api1: RestApi, api2: RestApi) extends BootedNode with RouteConcatenation {
      override lazy val api = Some({ ec: ExecutionContext ⇒ api1(ec) ~ api2(ec) })
    }
  }

}

/**
 * All microservice implementations should extend this class, providing the microservice name,
 * @param microserviceProps the microservice properties
 */
abstract class MicroserviceApp(microserviceProps: MicroserviceProps) extends App {

  def boot(implicit system: ActorSystem, cluster: Cluster): BootedNode

  import com.eigengo.lift.common.MicroserviceApp._

  private val name = "Lift"

  def startup(): Unit = {
    // resolve the local host name
    // load config and set Up etcd client
    val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=${microserviceProps.role}")
    val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=['${microserviceProps.role}']")
    val config = clusterShardingConfig.withFallback(clusterRoleConfig).withFallback(ConfigFactory.load())

    implicit val system = ActorSystem(name, config)
    val log = Logger(getClass)

    val hostname = InetAddress.getLocalHost.getHostAddress
    log.info(s"Starting Up microservice $microserviceProps at $hostname")
    Thread.sleep(10000)

    // Create the ActorSystem for the microservice
    log.info("Creating the microservice's ActorSystem")
    val cluster = Cluster(system)
    ClusterStartup(system).join {
      val selfAddress = cluster.selfAddress
      log.info(s"Node $selfAddress booting up")
      // boot the microservice code
      val bootedNode = boot(system, cluster)
      log.info(s"Node $selfAddress booted up $bootedNode")
      bootedNode.api.foreach { api ⇒
        val route: Route = api(system.dispatcher)
        val port: Int = 8080
        val restService = system.actorOf(Props(classOf[RestAPIActor], route))
        IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = port)
      }
      // logme!
      log.info(s"Node $selfAddress Up")
    }
  }

  startup()
}
