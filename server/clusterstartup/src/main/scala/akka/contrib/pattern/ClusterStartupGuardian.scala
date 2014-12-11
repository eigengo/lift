package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster

object ClusterStartupGuardian {
  val props = Props[ClusterStartupGuardian]

  case class TryJoinSeedNodes(cluster: Cluster)
  case class RemoveCluster(cluster: Cluster)
  case class MarkClusterUp(cluster: Cluster)
}

class ClusterStartupGuardian extends Actor with ActorLogging {
  import ClusterStartupGuardian._
  import scala.concurrent.duration._
  private val inventory = ClusterInventory(context.system)
  private var seedNodes: Set[Address] = Set.empty
  private val retryDuration = 5.seconds
  private val minNrMembers = 2
  import context.dispatcher

  inventory.subscribe("node", self, refresh = true)

  override def receive: Receive = {
    case ClusterInventoryGuardian.KeyAdded(_, value) ⇒ value match {
      case AddressFromURIString(address) ⇒
        seedNodes = seedNodes + address
        log.info(s"Now with seed nodes $seedNodes")
      case x ⇒ log.warning(s"Got value $value, which is not address")
    }

    case cmd@TryJoinSeedNodes(cluster) ⇒
      if (seedNodes.size < minNrMembers) {
        log.warning(s"Could not get sufficient number of seed nodes (got ${seedNodes.size}, needed $minNrMembers). Retrying in $retryDuration.")
        context.system.scheduler.scheduleOnce(retryDuration, self, cmd)
      } else {
        log.info(s"Joining seed nodes $seedNodes")
        inventory.unsubscribe("node", self)
        cluster.joinSeedNodes(seedNodes.toList.sortWith((x, y) ⇒ x.toString.compareTo(y.toString) < 0).take(minNrMembers))
      }

  }

}
