package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster

object ClusterStartup extends ExtensionId[ClusterStartup] with ExtensionIdProvider {
  override def get(system: ActorSystem): ClusterStartup = super.get(system)

  override def lookup = ClusterInventory

  override def createExtension(system: ExtendedActorSystem): ClusterStartup =
    new ClusterStartup(system)

}

/**
 * XXX
 *
 * @param system
 */
class ClusterStartup(system: ExtendedActorSystem) extends Extension {
  import akka.contrib.pattern.ClusterStartupGuardian._

  private val cluster = Cluster(system)
  private lazy val guardian = system.actorOf(ClusterStartupGuardian.props)

  def join[U](onMemberUp: ⇒ U): Unit = {
    ClusterInventory(system).set("node", cluster.selfAddress.toString)
    guardian ! JoinSeedNodes(cluster)
    cluster.registerOnMemberUp(onMemberUp)
  }

}
