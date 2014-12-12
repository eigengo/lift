package akka.contrib.pattern.inventorystore

/**
 * Missing key ``key``
 * @param key the key
 */
private[akka] case class NoSuchKeyException(key: String) extends RuntimeException(s"No such key $key")
