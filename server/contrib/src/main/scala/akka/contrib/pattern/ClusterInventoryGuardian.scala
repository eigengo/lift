package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberExited, MemberRemoved}
import akka.contrib.pattern.inventorystore.InventoryStore
import com.typesafe.config.Config

import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Companion object for the guardian actor
 */
private[akka] object ClusterInventoryGuardian {

  /**
   * Add the key and value
   * @param key the key
   * @param value the value
   */
  case class AddValue(key: String, value: String)

  /**
   * Remove all added keys: this should typically be called at shutdown
   */
  case object RemoveAllAddedKeys

  /**
   * Returns the ``Props`` to create the ``ClusterInventoryGuardian``
   * @param config the configuration
   * @param system the ActorSystem
   * @return the Props
   */
  def props(config: Config, system: ActorSystem): Props = {
    val store = InventoryStore(config, system)
    val rootKey = config.getString("root-key")
    Props(classOf[ClusterInventoryGuardian], rootKey, store)
  }

  /* Refresh all subscribers on timer */
  private case object RefreshSubscribers

  /* Subscriber record */
  private case class Subscriber(key: String, subscriber: ActorRef)
}

/**
 * The actual manager of the ClusterInventory, taking the ``rootKey``, which will be pre-pended to all keys, and the
 * ``inventoryStore``, which provides the CRUD mechanism for the keys and values
 *
 * @param rootKey the root key
 * @param inventoryStore the store
 */
private[akka] class ClusterInventoryGuardian(rootKey: String, inventoryStore: InventoryStore) extends Actor with ActorLogging {
  import akka.contrib.pattern.ClusterInventory._
  import akka.contrib.pattern.ClusterInventoryGuardian._
  import context.dispatcher
  import scala.concurrent.duration._

  private val refreshDuration = 5.seconds
  private val cluster = Cluster(context.system)
  private var addedKeys: List[String] = Nil
  private var subscribers: List[Subscriber] = Nil

  // Subscribe to cluster events for automatic cleanup
  cluster.subscribe(self, classOf[MemberExited], classOf[MemberRemoved])
  context.system.scheduler.schedule(1.second, refreshDuration, self, RefreshSubscribers)

  /**
   * Compute the suffix for the given ``address``, which will be added to all created nodes
   * @param address the address, typically ``cluster.selfAddress``.
   * @return the suffix
   */
  private def suffixForAddress(address: Address): String = {
    s"${address.protocol.replace(':', '_')}_${address.host.getOrElse("")}_${address.port.getOrElse(0)}"
  }

  /**
   * Removes all keys created for the given address.
   * @param address the address
   */
  private def removeKeysForAddress(address: Address): Unit = {
    val suffix = suffixForAddress(address)
    inventoryStore.getAll(rootKey).onComplete {
      case Success(kvs) ⇒ kvs.foreach {
        case (key, _) ⇒ if (key.endsWith(suffix)) {
          log.debug(s"Removing $key.")
          inventoryStore.delete(key)
        }
      }
      case Failure(_) ⇒ log.error("Could not get all keys.")
    }
  }

  override def receive: Receive = {
    /* Subscribe (local) subscriber to the key, but only once.*/
    case Subscribe(key, subscriber) if !subscribers.exists(_.subscriber == sender()) ⇒
      val resolvedKey = rootKey + "/" + key
      subscribers = Subscriber(resolvedKey, subscriber) :: subscribers
      log.debug(s"Subscribed $subscriber to $resolvedKey. Now with $subscribers.")

    /* Unsubscribe subscriber from updates to key */
    case Unsubscribe(key, subscriber) ⇒
      val resolvedKey = rootKey + "/" + key
      subscribers = subscribers.dropWhile(s ⇒ s.key == resolvedKey && s.subscriber == subscriber)
      log.debug(s"Unsubscribed $subscriber from $resolvedKey. Now with $subscribers.")

    /* Send updates to all subscribers */
    case RefreshSubscribers ⇒
      val uniqueKeys = subscribers.map(_.key).distinct  // don't query the same key multiple times
      uniqueKeys.foreach { key ⇒
        inventoryStore.getAll(key).onComplete {
          case Success(nodes) ⇒
            subscribers.foreach { sub ⇒
              if (sub.key == key) {
                log.debug(s"KeyValuesRefreshed with $nodes to $sub")
                sub.subscriber ! KeyValuesRefreshed(nodes)
              }
            }
          case Failure(ex) ⇒ log.error(s"Could retrieve all nodes for $key: ${ex.getMessage}")
        }
      }

    /* Add a value for the given key */
    case AddValue(key, value) ⇒
      val resolvedKey = rootKey + "/" + key + "/" + suffixForAddress(cluster.selfAddress)
      inventoryStore.set(resolvedKey, value).onComplete {
        case Success(_) ⇒
          addedKeys = resolvedKey :: addedKeys    // keep track of the added keys so we can remove them
          subscribers.foreach { subscriber ⇒     // ping the subscribers
            if (key.startsWith(subscriber.key)) subscriber.subscriber ! KeyAdded(key, value)
          }
        case Failure(ex) ⇒ log.error(s"Could not set $resolvedKey to $value: ${ex.getMessage}")
      }

    /* Cluster members leaving */
    case MemberExited(member) ⇒
      log.debug(s"Member at ${member.address} exited. Removing its keys.")
      removeKeysForAddress(member.address)

    /* Cluster members leaving */
    case MemberRemoved(member, _) ⇒
      log.debug(s"Member at ${member.address} removed. Removing its keys.")
      removeKeysForAddress(member.address)

    /* Remove all keys we have added here */
    case RemoveAllAddedKeys ⇒
      addedKeys.foreach(inventoryStore.delete)
  }
}
