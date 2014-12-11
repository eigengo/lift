package akka.contrib.pattern

import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

case class NoSuchKeyException(key: String) extends RuntimeException(s"No such key $key")

case class InventoryStoreNodeAttribute(name: String, value: String)
case class InventoryStoreNode(name: String, value: String, attributes: Seq[InventoryStoreNodeAttribute])

object InventoryStore {
  def apply(config: Config): InventoryStore = config.getString("plugin") match {
      case "file" ⇒ new FileInventoryStore(config.getString("file.dir"))
      case "etcd" ⇒ new EtcdInventoryStore(config.getString("etcd.url"))
  }
}

trait InventoryStore {

  def get(key: String): Future[InventoryStoreNode]

  def getAll(key: String): Future[List[InventoryStoreNode]]

  def set(path: String, node: InventoryStoreNode): Future[Unit]

  def delete(path: String): Future[Unit]

}

object FileInventoryStore {

  private case class KISN(key: String, isn: InventoryStoreNode)

  private def parseKisn(s: String): Option[KISN] = {
    Try {
      val cfg = ConfigFactory.parseString(s)
      Some(KISN(cfg.getString("key"), InventoryStoreNode(cfg.getString("name"), cfg.getString("value"), Nil)))
    }.getOrElse(None)
  }

  private def kisnToString(kisn: KISN): String = {
    s"""
       |${kisn.key} {
       |  name =  "${kisn.isn.name}"
       |  value = "${kisn.isn.value}"
       |}
     """.stripMargin
  }

  implicit object KVOrdering extends Ordering[KISN] {
    override def compare(x: KISN, y: KISN): Int = x.key.compareTo(y.key)
  }
}

class FileInventoryStore(dir: String) extends InventoryStore {
  import java.io.{File, FileOutputStream}
  import akka.contrib.pattern.FileInventoryStore._
  val inventory = new File(dir + "inventory.txt")

  private def load(): List[KISN] = {
    Source.fromFile(inventory).getLines().flatMap(parseKisn).toList.sorted
  }

  private def save(kvs: List[KISN]): Unit = {
    val bytes = kvs.map(kisnToString).mkString("\n").getBytes("UTF-8")
    val fos = new FileOutputStream(inventory, false)
    fos.write(bytes)
    fos.close()
  }

  override def get(key: String): Future[InventoryStoreNode] = {
    load().find(_.key == key).fold(Future.failed(NoSuchKeyException(key)))(x ⇒ Future.successful(x.isn))
  }

  override def set(path: String, node: InventoryStoreNode): Future[Unit] = ???

  override def getAll(key: String): Future[List[InventoryStoreNode]] = ???

  override def delete(path: String): Future[Unit] = ???
}

class EtcdInventoryStore(url: String) extends InventoryStore {
  override def get(key: String): Future[InventoryStoreNode] = ???

  override def set(path: String, node: InventoryStoreNode): Future[Unit] = ???

  override def getAll(key: String): Future[List[InventoryStoreNode]] = ???

  override def delete(path: String): Future[Unit] = ???
}

/*
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
 */