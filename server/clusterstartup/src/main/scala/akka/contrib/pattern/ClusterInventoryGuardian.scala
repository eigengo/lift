package akka.contrib.pattern

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberRemoved, MemberExited}
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ClusterInventoryGuardian {
  case class AddValue(key: String, value: String)
  case object RemoveAllAddedKeys

  def props(config: Config, system: ActorSystem): Props = {
    val store = InventoryStore(config, system)
    val rootKey = config.getString("root-key")
    Props(classOf[ClusterInventoryGuardian], rootKey, store)
  }

  case class Subscribe(keyPattern: String, subscriber: ActorRef, refresh: Boolean)
  case class Unsubscribe(keyPattern: String, subscriber: ActorRef)
  case class KeyAdded(key: String, value: String)
  case class KeyRemoved(key: String)

  private case object RefreshSubscribers
  private case class Subscriber(key: String, subscriber: ActorRef, refresh: Boolean)
}

class ClusterInventoryGuardian(rootKey: String, inventoryStore: InventoryStore) extends Actor with ActorLogging {
  import akka.contrib.pattern.ClusterInventoryGuardian._
  import scala.concurrent.duration._
  import context.dispatcher

  private val cluster = Cluster(context.system)
  private var addedKeys: List[String] = Nil
  private var subscribers: List[Subscriber] = Nil

  cluster.subscribe(self, classOf[MemberExited], classOf[MemberRemoved])

  private def suffixForCluster(address: Address): String = {
    s"${address.protocol.replace(':', '_')}_${address.host.getOrElse("")}_${address.port.getOrElse(0)}"
  }

  private def notifyRemoved(key: String): Unit = {
    subscribers.foreach { subscriber ⇒
      if (key.startsWith(subscriber.key)) subscriber.subscriber ! KeyRemoved(key)
    }
  }

  override def receive: Receive = {
    case Subscribe(key, subscriber, refresh) if !subscribers.exists(_.subscriber == sender()) ⇒
      val resolvedKey = rootKey + "/" + key
      log.info(s"Subscribed $subscriber to $resolvedKey")
      subscribers = Subscriber(resolvedKey, subscriber, refresh) :: subscribers
      if (refresh) context.system.scheduler.scheduleOnce(5.seconds, self, RefreshSubscribers)

    case Unsubscribe(key, subscriber) ⇒
      val resolvedKey = rootKey + "/" + key
      log.info(s"Unsubscribed $subscriber from $resolvedKey ($subscribers)")
      subscribers = subscribers.dropWhile(s ⇒ s.key == resolvedKey && s.subscriber == subscriber)
      log.info(s"Unsubscribed $subscriber from $resolvedKey ($subscribers)")

    case RefreshSubscribers ⇒
      subscribers.foreach { sub ⇒
        inventoryStore.getAll(sub.key).onComplete {
          case Success(nodes) ⇒
            nodes.foreach { case (k, v) ⇒ if (sub.refresh) sub.subscriber ! KeyAdded(k, v) }
          case Failure(exn) ⇒ // noop
        }
      }
      if (subscribers.exists(_.refresh)) context.system.scheduler.scheduleOnce(5.seconds, self, RefreshSubscribers)

    case AddValue(key, value) ⇒
      val resolvedKey = rootKey + "/" + key + "/" + suffixForCluster(cluster.selfAddress)
      inventoryStore.set(resolvedKey, value).onComplete {
        case Success(_) ⇒
          addedKeys = resolvedKey :: addedKeys
          subscribers.foreach { subscriber ⇒
            if (key.startsWith(subscriber.key)) subscriber.subscriber ! KeyAdded(key, value)
          }
        case Failure(ex) ⇒ log.error(s"Could not set $resolvedKey to $value: ${ex.getMessage}")
      }

    case MemberExited(member) ⇒
      log.info(s"Member at ${member.address} exited. Removing its keys.")
      val suffix = suffixForCluster(member.address)
      inventoryStore.getAll(rootKey).onComplete {
        case Success(kvs) ⇒ kvs.foreach {
          case (key, _) ⇒ if (key.endsWith(suffix)) {
            log.info(s"Removing $key.")
            inventoryStore.delete(key)
            notifyRemoved(key)
          }
        }
      }

    case MemberRemoved(member, _) ⇒
      log.info(s"Member at ${member.address} removed. Removing its keys.")
      val suffix = suffixForCluster(member.address)
      inventoryStore.getAll(rootKey).onComplete {
        case Success(kvs) ⇒
          kvs.foreach {
            case (key, _) ⇒ if (key.endsWith(suffix)) {
              log.info(s"Removing $key.")
              inventoryStore.delete(key)
              notifyRemoved(key)
            }
          }
      }

    case RemoveAllAddedKeys ⇒
      addedKeys.foreach { key ⇒
        inventoryStore.delete(key)
        notifyRemoved(key)
      }
  }
}
