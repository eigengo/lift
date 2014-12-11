package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster

object ClusterInventory extends ExtensionId[ClusterInventory] with ExtensionIdProvider {
  override def get(system: ActorSystem): ClusterInventory = super.get(system)

  override def lookup = ClusterInventory

  override def createExtension(system: ExtendedActorSystem): ClusterInventory =
    new ClusterInventory(system)

}


/**
 * XXX
 *
 * Settings are
 * {{{
 * akka.contrib.cluster.inventory {
 *   root-key = "/system/frontent.cluster.nodes"
 *   plugin = "file"
 *   file {
 *     path = "target/inventory"
 *   }
 * }
 * }}}
 *
 * @param system
 */
class ClusterInventory(system: ExtendedActorSystem) extends Extension {
  import akka.contrib.pattern.ClusterInventoryGuardian._

  private val cluster = Cluster(system)
  /**
   * INTERNAL API
   */
  private[akka] object Settings {
    val config = system.settings.config.getConfig("akka.contrib.cluster.inventory")
    val Inventory = config.getConfig("inventory")
  }
  private lazy val guardian = system.actorOf(ClusterInventoryGuardian.props(Settings.Inventory, system))
  system.registerOnTermination(leave())

  def add(key: String, value: String): Unit = {
    guardian ! AddValue(cluster.selfAddress + "/" + key, value)
  }

  def subscribe(keyPattern: String, subscriber: ActorRef): Unit = {
    guardian ! Subscribe(keyPattern, subscriber)
  }

  private def leave(): Unit = {
    guardian ! RemoveAllKeys(cluster.selfAddress.toString)
  }

}
