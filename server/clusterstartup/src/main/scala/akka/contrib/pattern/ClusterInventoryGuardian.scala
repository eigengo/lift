package akka.contrib.pattern

import akka.actor._
import com.typesafe.config.Config

import scala.language.postfixOps

object ClusterInventoryGuardian {
  case class AddValue(key: String, value: String)
  case class RemoveAllKeys(prefix: String)

  def props(config: Config, system: ActorSystem): Props = {
    val store = InventoryStore(config, system)
    val rootKey = config.getString("root-key")
    Props(classOf[ClusterInventoryGuardian], rootKey, store)
  }
}

class ClusterInventoryGuardian(rootKey: String, minNrMembers: Int, inventoryStore: InventoryStore) extends Actor with ActorLogging {
  import akka.contrib.pattern.ClusterInventoryGuardian._
  import ClusterInventory._
  import scala.concurrent.duration._
  val retryDuration = 5.seconds
  var subscribers: List[(String, ActorRef)] = Nil

  override def receive: Receive = {
    case Subscribe(key) ⇒
      subscribers = (key → sender()) :: subscribers

    case AddValue(key, value) ⇒
      inventoryStore.set(s"$rootKey/$key", InventoryStoreNode(value, Nil))
      subscribers.foreach {
        case (k, s) ⇒ if (key.startsWith(k)) s ! KeyAdded(key, value)
      }

    case RemoveAllKeys(key) ⇒
      inventoryStore.delete(key)
      subscribers.foreach {
        case (k, s) ⇒ if (key.startsWith(k)) s ! KeyRemoved(key)
      }
  }
}
