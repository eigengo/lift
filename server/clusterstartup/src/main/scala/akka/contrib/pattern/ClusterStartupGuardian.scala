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
  ClusterInventory(context.system).subscribe("node", self, Some(5.seconds))
  private val selfCluster = Cluster(context.system)
  private var seedNodes: List[Address] = List.empty
  private val retryDuration = 5.seconds
  private val minNrMembers = 2
  import context.dispatcher

  override def receive: Receive = {
    case ClusterInventoryGuardian.KeyAdded(_, value) ⇒ value match {
      case AddressFromURIString(address) if selfCluster.selfAddress != address ⇒
        seedNodes = address :: seedNodes
      case x ⇒ log.warning(s"Got key $value, which is not address")
    }

    case cmd@TryJoinSeedNodes(cluster) ⇒
      if (seedNodes.size < minNrMembers) {
        log.warning(s"Could not get sufficient number of seed nodes (got ${seedNodes.size}, needed $minNrMembers). Retrying in $retryDuration.")
        context.system.scheduler.scheduleOnce(retryDuration, self, cmd)
      } else {
        log.info(s"Got seed nodes $seedNodes")
        cluster.joinSeedNodes(seedNodes)
      }

  }

}
