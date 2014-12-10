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

    import scala.concurrent.duration._
    val etcdUrl: String = config.getString("etcd.url")
    val etcd = new EtcdClient(etcdUrl)
    log.info(s"Config loaded; etcd expected at $etcdUrl")

    // retry timeout for the cluster formation
    val retry = config.getDuration("akka.cluster.retry", TimeUnit.SECONDS).seconds
    val minNrMembers = config.getInt("akka.cluster.min-nr-of-members")

    // Create the ActorSystem for the microservice
    log.info("Creating the microservice's ActorSystem")
    val cluster = Cluster(system)
    val selfAddress: Address = cluster.selfAddress

    import system.dispatcher

    // register shutdown callback
    system.registerOnTermination(shutdown())

    // register this (cluster) actor system with etcd
    etcd.setKey(EtcdKeys.ClusterNodes(cluster), selfAddress.toString).onComplete {
      case Success(_) =>
        // Register cluster MemberUp callback
        cluster.registerOnMemberUp {
          log.info(s"Node $selfAddress booting up")
          // boot the microservice code
          val bootedNode = boot(system, cluster)
          log.info(s"Node $selfAddress booted up $bootedNode")
          bootedNode.api.foreach(startupApi)
          // logme!
          log.info(s"Node $selfAddress Up")
        }
        joinCluster()

      case Failure(exn) =>
        log.error(s"Failed to set state to 'Joining' with etcd: $exn")
        shutdown()
    }

    def startupApi(api: ExecutionContext ⇒ Route): Unit = {
      import AdapterProtocol._
      val route: Route = api(system.dispatcher)
      val port: Int = 8080
      // TODO: We're not always 1.0 C&Q
      val restApi = RestApi(hostname, port, "1.0", Seq(Query, Command))
      AdapterProtocol.register(selfAddress, restApi)
      system.registerOnTermination(AdapterProtocol.unregister(selfAddress))
      val restService = system.actorOf(Props(classOf[RestAPIActor], route))
      IO(Http)(system) ! Http.Bind(restService, interface = "0.0.0.0", port = port)
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
      cluster.leave(selfAddress)
      log.info(s"Shut down ActorSystem $system")
      system.shutdown()
    }
  }

  startup()
}
