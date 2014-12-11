package akka.contrib.pattern

import akka.actor._
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.util.Success

object ClusterInventoryGuardian {
  case class AddValue(key: String, value: String)
  case class RemoveAllKeys(prefix: String)

  def props(config: Config, system: ActorSystem): Props = {
    val store = InventoryStore(config, system)
    val rootKey = config.getString("root-key")
    Props(classOf[ClusterInventoryGuardian], rootKey, store)
  }

  case class Subscribe(keyPattern: String, subscriber: ActorRef, refresh: Option[FiniteDuration] = {})
  case class KeyAdded(key: String, value: String)
  case class KeyRemoved(key: String)


  private case class RefreshSubscriber(keyPattern: String, subsciber: ActorRef)
  private case class Subscriber(keyPattern: String, subscriber: ActorRef)
}

class ClusterInventoryGuardian(rootKey: String, minNrMembers: Int, inventoryStore: InventoryStore) extends Actor with ActorLogging {
  import akka.contrib.pattern.ClusterInventoryGuardian._

import scala.concurrent.duration._
  val retryDuration = 5.seconds
  var subscribers: List[Subscriber] = List.empty

  override def receive: Receive = {
    case Subscribe(key, subscriber, refresh) if !subscribers.exists(_.subscriber == sender()) ⇒
      subscribers = Subscriber(key, subscriber) :: subscribers
      refresh.foreach(d ⇒ context.system.scheduler.schedule(1.second, 5.seconds, self, RefreshSubscriber(key, subscriber)))

    case RefreshSubscriber(keyPattern, subscriber) ⇒
      inventoryStore.getAll(keyPattern).onComplete {
        case Success(nodes) ⇒ nodes.foreach { node ⇒ subscriber ! KeyAdded(keyPattern, node.value) }
      }

    case AddValue(key, value) ⇒
      inventoryStore.set(s"$rootKey/$key", InventoryStoreNode(value, Nil))
      subscribers.foreach { subscriber ⇒
        if (key.startsWith(subscriber.keyPattern)) subscriber.subscriber ! KeyAdded(key, value)
      }

    case RemoveAllKeys(key) ⇒
      inventoryStore.delete(key)
      subscribers.foreach { subscriber ⇒
        if (key.startsWith(subscriber.keyPattern)) subscriber.subscriber ! KeyRemoved(key)
      }
  }
}
