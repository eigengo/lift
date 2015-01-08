package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster

/**
 * Companion for the ``ClusterStartupGuardian``, defining the startup props and the messages sent to it
 */
private[akka] object ClusterStartupGuardian {
  val props = Props[ClusterStartupGuardian]

  /**
   * Join the discovered seed nodes in the cluster
   * @param cluster the cluster
   */
  case class JoinSeedNodes(cluster: Cluster)

  /**
   * Remove the given node from the cluster
   * @param cluster the node to be removed
   */
  case class RemoveCluster(cluster: Cluster)
}

private[akka] class ClusterStartupGuardian extends Actor with ActorLogging {
  import ClusterStartupGuardian._
  import scala.concurrent.duration._
  private val inventory = ClusterInventory(context.system)
  private var seedNodes: Set[Address] = Set.empty
  private val retryDuration = 5.seconds
  private val minNrMembers = 2
  import context.dispatcher

  inventory.subscribe("node", self)

  override def receive: Receive = {
    case ClusterInventory.KeyValuesRefreshed(kvs) ⇒
      kvs.foreach {
        case (_, value) ⇒ value match {
          case AddressFromURIString(address) ⇒
            seedNodes = seedNodes + address
            log.debug(s"Now with seed nodes $seedNodes")
          case x ⇒ log.warning(s"Got value $value, which is not address")
        }
      }

    case cmd@JoinSeedNodes(cluster) ⇒
      if (seedNodes.size < minNrMembers) {
        log.warning(s"Could not get sufficient number of seed nodes (got ${seedNodes.size}, needed $minNrMembers). Retrying in $retryDuration.")
        context.system.scheduler.scheduleOnce(retryDuration, self, cmd)
      } else {
        log.debug(s"Joining seed nodes $seedNodes")
        inventory.unsubscribe("node", self)
        cluster.joinSeedNodes(seedNodes.toList.sortWith((x, y) ⇒ x.toString.compareTo(y.toString) < 0).take(minNrMembers))
      }

  }

}
