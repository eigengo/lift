package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster
import com.typesafe.config.Config

import scala.language.postfixOps
import scala.util.{Failure, Success}

object ClusterStartupMediator {
  case class TryJoinSeedNodes(cluster: Cluster)
  case class RemoveCluster(cluster: Cluster)
  case class MarkClusterUp(cluster: Cluster)

  def props(config: Config): Props = {
    val store = InventoryStore(config)
    val rootKey = config.getString("root-key")
    val minNrMembers = config.getInt("akka.cluster.min-nr-of-members")
    Props(classOf[ClusterStartupMediator], rootKey, minNrMembers, store)
  }
}

class ClusterStartupMediator(rootKey: String, minNrMembers: Int, inventoryStore: InventoryStore) extends Actor with ActorLogging {
  import akka.contrib.pattern.ClusterStartupMediator._

import scala.concurrent.duration._
  val retryDuration = 5.seconds
  import context.dispatcher

  override def receive: Receive = {
    case cmd@TryJoinSeedNodes(cluster) ⇒
      inventoryStore.getAll(rootKey).onComplete {
        case Success(nodes) ⇒
          val seedNodes = nodes.map(x ⇒ AddressFromURIString(x.value)).filter(cluster.selfAddress !=).take(minNrMembers)
          if (seedNodes.size < minNrMembers) {
            log.warning(s"Could not get sufficient number of seed nodes (got ${seedNodes.size}, needed $minNrMembers). Retrying in $retryDuration.")
            context.system.scheduler.schedule(1.second, retryDuration, self, cmd)
          } else {
            log.info(s"Got seed nodes $seedNodes")
            cluster.joinSeedNodes(seedNodes)
          }
        case Failure(exn)   ⇒
          log.warning(s"Could not read the available nodes. Retrying in $retryDuration.")
          context.system.scheduler.schedule(1.second, retryDuration, self, cmd)
      }

    case RemoveCluster(cluster) ⇒
      inventoryStore.delete(s"$rootKey/${cluster.selfUniqueAddress.uid}")

    case MarkClusterUp(cluster) ⇒
      inventoryStore.set(s"$rootKey", InventoryStoreNode(cluster.selfUniqueAddress.uid.toString, cluster.selfAddress.toString, Nil))
      inventoryStore.set(s"$rootKey/${cluster.selfUniqueAddress.uid}", InventoryStoreNode("status", "UP", Nil))
  }
}
