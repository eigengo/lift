package com.eigengo.lift.common

import java.net.InetAddress

import akka.actor._
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterInventory.UnresolvedDependencies
import akka.contrib.pattern.{ClusterStartup, ClusterInventory}
import akka.io.IO
import com.eigengo.lift.common.MicroserviceApp.{BootedNode, MicroserviceProps}
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.routing.{HttpServiceActor, Route, RouteConcatenation}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Companion for the microservice app
 */
object MicroserviceApp {

  sealed trait CQRSMicroservice
  case object CommandMicroservice extends CQRSMicroservice {
    override val toString = "c"
  }
  case object QueryMicroservice extends CQRSMicroservice {
    override val toString = "q"
  }

  /**
   * The microservice props
   * @param name the microservice name
   * @param version the version (try semantic versioning, but we don't really care here)
   * @param dependencies the other microservices the must exist in the inventory for this one to start-up
   * @param cqrs the sides of the CQRS barricade
   */
  case class MicroserviceProps(name: String,
                               version: String = "1.0",
                               dependencies: Seq[String] = Nil,
                               cqrs: Seq[CQRSMicroservice] = Seq(CommandMicroservice, QueryMicroservice)) {
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
    import com.eigengo.lift.common.MicroserviceApp.BootedNode._
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

  /**
   * Subclasses must implement this method to perform their startup: all dependencies
   * on other microservices are fully resolved by then
   * @param system the local ActorSystem
   * @param cluster the cluster hosting the AS
   */
  def boot(implicit system: ActorSystem, cluster: Cluster): BootedNode

  import com.eigengo.lift.common.MicroserviceApp._
  import scala.concurrent.duration._

  private val name = "Lift"

  def startup(): Unit = {
    // resolve the local host name
    // load config and set Up etcd client
    val clusterShardingConfig = ConfigFactory.parseString(s"akka.contrib.cluster.sharding.role=${microserviceProps.role}")
    val clusterRoleConfig = ConfigFactory.parseString(s"akka.cluster.roles=[${microserviceProps.role}]")
    val config = clusterShardingConfig.withFallback(clusterRoleConfig).withFallback(ConfigFactory.load())

    implicit val system = ActorSystem(name, config)
    import system.dispatcher
    val cluster = Cluster(system)
    val log = Logger(getClass)

    ClusterInventory(system).resolveDependencies(microserviceProps.dependencies, 60.seconds).onComplete {
      case Success(_) ⇒
        val hostname = InetAddress.getLocalHost.getHostAddress
        log.info(s"Starting Up microservice $microserviceProps at $hostname")
        Thread.sleep(10000)

        // Create the ActorSystem for the microservice
        log.info("Creating the microservice's ActorSystem")
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
            ClusterInventory(system).set("api", s"http://$hostname:$port?version=${microserviceProps.version}&side=${microserviceProps.cqrs.mkString(",")}")
          }
          // logme!
          log.info(s"Node $selfAddress Up")
        }
      case Failure(UnresolvedDependencies(resolved, remaining)) ⇒
        log.error(s"Could not resolve dependencies for $microserviceProps: resolved $resolved, $remaining remaining.")
        system.shutdown()
      case Failure(ex) ⇒
        log.error(s"Could start the $microserviceProps: ${ex.getMessage}.")
        system.shutdown()
    }
  }

  startup()
}
