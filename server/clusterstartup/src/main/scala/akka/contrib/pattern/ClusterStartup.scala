package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster

object ClusterStartup extends ExtensionId[ClusterStartup] with ExtensionIdProvider {
  override def get(system: ActorSystem): ClusterStartup = super.get(system)

  override def lookup = ClusterStartup

  override def createExtension(system: ExtendedActorSystem): ClusterStartup =
    new ClusterStartup(system)
}


/**
 * XXX
 *
 * Settings are
 * {{{
 * akka.contrib.cluster.startup {
 *   inventory {
 *     root-key = "/system/frontent.cluster.nodes"
 *     plugin = "file"
 *     file {
 *       dir = "target/inventory"
 *     }
 *   }
 * }
 * }}}
 *
 * @param system
 */
class ClusterStartup(system: ExtendedActorSystem) extends Extension {
  import akka.contrib.pattern.ClusterStartupMediator._

  private val cluster = Cluster(system)
  /**
   * INTERNAL API
   */
  private[akka] object Settings {
    val config = system.settings.config.getConfig("akka.contrib.cluster.startup")
    // local-file | etcd
    val Inventory = config.getConfig("inventory")
  }
  private lazy val mediator = system.actorOf(ClusterStartupMediator.props(Settings.Inventory))
  system.registerOnTermination(leave())
  cluster.registerOnMemberUp(markUp())

  def join[U](onMemberUp: ⇒ U): Unit = {
    cluster.registerOnMemberUp(onMemberUp)
    mediator ! TryJoinSeedNodes(cluster)
  }

  private def leave(): Unit = {
    mediator ! RemoveCluster(cluster)
  }

  private def markUp(): Unit = {
    mediator ! MarkClusterUp(cluster)
  }
}
