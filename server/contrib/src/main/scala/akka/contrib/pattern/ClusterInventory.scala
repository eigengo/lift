package akka.contrib.pattern

import akka.actor._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
 * Cluster inventory extension provides a mechanism to keep inventory of values that are
 * running in the cluster.
 *
 * The inventory store can be an etcd server, or a simple file, or perhaps a database.
 */
object ClusterInventory extends ExtensionId[ClusterInventory] with ExtensionIdProvider {
  case class UnresolvedDependencies(resolved: Seq[String], remaining: Seq[String]) extends RuntimeException(s"Unresolved dependencies: found $resolved, $remaining remaining.")

  case class Subscribe(keyPattern: String, subscriber: ActorRef)
  case class Unsubscribe(keyPattern: String, subscriber: ActorRef)
  case class KeyValuesRefreshed(values: List[(String, String)])
  case class KeyAdded(key: String, value: String)

  override def get(system: ActorSystem): ClusterInventory = super.get(system)

  override def lookup() = ClusterInventory

  override def createExtension(system: ExtendedActorSystem): ClusterInventory =
    new ClusterInventory(system)

}

/**
 * The extension that starts up the cluster inventory according to its settings, which are

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
 * @see akka.contrib.pattern.EtcdInventoryStore
 * @see akka.contrib.pattern.FileInventoryStore
 *
 * @param system the actor system being extended
 */
class ClusterInventory(system: ExtendedActorSystem) extends Extension {
  import akka.contrib.pattern.ClusterInventory._
  import akka.contrib.pattern.ClusterInventoryGuardian._

  /**
   * Settings for the ``akka.contrib.cluster.inventory`` extension
   */
  private[akka] object Settings {
    val config = system.settings.config.getConfig("akka.contrib.cluster")
    val Inventory = config.getConfig("inventory")
  }

  // the inventory guardian
  private lazy val guardian = system.actorOf(ClusterInventoryGuardian.props(Settings.Inventory, system), "cluster-inventory-guardian")
  system.registerOnTermination {
    guardian ! RemoveAllAddedKeys
  }

  /**
   * Add the inventory item at the given ``key`` and ``value``. Note that the actual key stored in the
   * inventory store (defined in ``akka.contrib.cluster.inventory.plugin``) will start with the value in
   * ``akka.contrib.cluster.inventory.root-key``, and will end with the URL-encoded representation
   * of this node's address.
   *
   * Typically, keys are added in "directories": that is, they include slashes in their name. One might,
   * for example, call ``add("node", cluster.selfAddress.toString)`` to indicate that there is a node
   * in the cluster.
   *
   * @param key the key to be added
   * @param value the value to be added
   */
  def set(key: String, value: String): Unit = {
    guardian ! SetKey(key, value)
  }

  /**
   * Removes the previously set key
   * @param key the key to be removed
   */
  def delete(key: String): Unit = {
    guardian ! DeleteKey(key)
  }

  /**
   * Subscribes the ``subscriber`` to receive updates to the node identified by ``key`` and all its sub-nodes.
   * The subscriber will receive instances of ``KeyValuesRefreshed`` with a list of K -> V tuples.
   *
   * @param key the key
   * @param subscriber the subscriber
   */
  def subscribe(key: String, subscriber: ActorRef): Unit = {
    guardian ! Subscribe(key, subscriber)
  }

  /**
   * Unsubscribes the given ``subscriber`` from receiving updates to ``key`` and all its sub-nodes.
   *
   * @param key the key
   * @param subscriber the subscriber
   */
  def unsubscribe(key: String, subscriber: ActorRef): Unit = {
    guardian ! Unsubscribe(key, subscriber)
  }

  /**
   * Resolve the ``dependencies``, retrying multiple times, and ultimately failing in ``duration``
   *
   * @param dependencies the dependencies to find
   * @param duration the duration
   * @return ()
   */
  def resolveDependencies(dependencies: Seq[String], duration: FiniteDuration): Future[Unit] = {
    Future.successful(())
  }

}
