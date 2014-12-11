package akka.contrib.pattern

import akka.actor._
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ClusterInventoryGuardian {
  case class AddValue(key: String, value: String)
  case class RemoveAllKeys(prefix: String)

  def props(config: Config, system: ActorSystem): Props = {
    val store = InventoryStore(config, system)
    val rootKey = config.getString("root-key")
    Props(classOf[ClusterInventoryGuardian], rootKey, store)
  }

  case class Subscribe(keyPattern: String, subscriber: ActorRef, refresh: Option[FiniteDuration] = None)
  case class KeyAdded(key: String, value: String)
  case class KeyRemoved(key: String)


  private case class RefreshSubscriber(keyPattern: String, subsciber: ActorRef)
  private case class Subscriber(keyPattern: String, subscriber: ActorRef)
}

class ClusterInventoryGuardian(rootKey: String, minNrMembers: Int, inventoryStore: InventoryStore) extends Actor with ActorLogging {
  import akka.contrib.pattern.ClusterInventoryGuardian._
  import scala.concurrent.duration._
  import context.dispatcher

  private var subscribers: List[Subscriber] = Nil


  override def receive: Receive = {
    case Subscribe(key, subscriber, refresh) if !subscribers.exists(_.subscriber == sender()) ⇒
      subscribers = Subscriber(key, subscriber) :: subscribers
      refresh.foreach(d ⇒ context.system.scheduler.schedule(1.second, d, self, RefreshSubscriber(key, subscriber)))

    case RefreshSubscriber(keyPattern, subscriber) ⇒
      inventoryStore.getAll(keyPattern).onComplete {
        case Success(nodes) ⇒ nodes.foreach { case (k, v) ⇒ subscriber ! KeyAdded(k, v) }
        case Failure(exn) ⇒ // noop
      }

    case AddValue(key, value) ⇒
      inventoryStore.set(s"$rootKey/$key", value)
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
