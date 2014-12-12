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
  case class KeyValue(key: String, value: String)
  case class KeyValuesRefreshed(values: List[(String, String)])
  case class KeyAdded(key: String, value: String)

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

  override def receive: Receive = {
    case Subscribe(key, subscriber, refresh) if !subscribers.exists(_.subscriber == sender()) ⇒
      val resolvedKey = rootKey + "/" + key
      subscribers = Subscriber(resolvedKey, subscriber, refresh) :: subscribers
      if (refresh) context.system.scheduler.scheduleOnce(5.seconds, self, RefreshSubscribers)
      log.info(s"Subscribed $subscriber to $resolvedKey. Now with $subscribers.")

    case Unsubscribe(key, subscriber) ⇒
      val resolvedKey = rootKey + "/" + key
      subscribers = subscribers.dropWhile(s ⇒ s.key == resolvedKey && s.subscriber == subscriber)
      log.info(s"Unsubscribed $subscriber from $resolvedKey. Now with $subscribers.")

    case RefreshSubscribers ⇒
      val uniqueKeys = subscribers.map(_.key).distinct
      uniqueKeys.foreach { key ⇒
        inventoryStore.getAll(key).onComplete {
          case Success(nodes) ⇒
            subscribers.foreach { sub ⇒
              if (sub.key == key) {
                log.info(s"KeyValuesRefreshed with $nodes to $sub")
                sub.subscriber ! KeyValuesRefreshed(nodes)
              }
            }
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
          }
        }
        case Failure(_) ⇒
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
            }
          }
        case Failure(_) ⇒
      }

    case RemoveAllAddedKeys ⇒
      addedKeys.foreach(inventoryStore.delete)
  }
}
