package akka.contrib.pattern.inventorystore

import akka.actor.ActorSystem
import com.typesafe.config.Config

import scala.concurrent.Future

/**
 * Inventory store plugin loader
 */
object InventoryStore {

  /**
   * Constructs the ``InventoryStore`` instance using the information in the ``config``
   *
   * @param config the configuration
   * @param system the system in case the store needs it
   * @return the constructed ``InventoryStore``
   */
  def apply(config: Config, system: ActorSystem): InventoryStore = config.getString("plugin") match {
      case "file" ⇒ new FileInventoryStore(config.getString("file.fileName"))
      case "etcd" ⇒ new EtcdInventoryStore(config.getString("etcd.url"), system)
  }
}

/**
 * Basic functionality of the inventory store. Think of the store as fancy K -> V store
 */
trait InventoryStore {

  /**
   * Get a value at the given ``key``
   * @param key the key
   * @return its value
   */
  def get(key: String): Future[String]

  /**
   * Get value at the given ``key`` and all its sub-keys
   * @param key the key
   * @return list of K -> Vs starting at ``key`` and descending
   */
  def getAll(key: String): Future[List[(String, String)]]

  /**
   * Set the ``value`` at the ``key``
   * @param key the key
   * @param value the value
   * @return Unit
   */
  def set(key: String, value: String): Future[Unit]

  /**
   * Remove the given ``key`` and all its sub-keys
   * @param key the key
   * @return Unit
   */
  def delete(key: String): Future[Unit]

}
