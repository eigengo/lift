package akka.contrib.pattern.inventorystore

import akka.actor.ActorSystem

import scala.concurrent.Future

private[akka] class EtcdInventoryStore(url: String, system: ActorSystem) extends InventoryStore {
  import EtcdClient._
  val etcdClient = new EtcdClient(url)(system)
  import system.dispatcher

  implicit class RichString(s: String) {
    def saneKey: String = if (s.startsWith("/")) s.substring(1) else s
  }

  override def get(key: String): Future[String] = {
    etcdClient.getKey(key).map(r ⇒ r.node.value.get)
  }

  override def set(key: String, value: String): Future[Unit] = {
    (if (key.contains("/")) {
      val i = key.lastIndexOf('/')
      val directory = key.substring(0, i)
      etcdClient.createDir(directory.saneKey).zip(etcdClient.setKey(key.saneKey, value)).map(_ ⇒ ())
    }
    else etcdClient.setKey(key.saneKey, value)).map(_ ⇒ ())
  }

  override def getAll(key: String): Future[List[(String, String)]] = {
    def append(nodes: List[NodeListElement]): List[(String, String)] = {
      nodes.flatMap { e ⇒
        val x = e.nodes.map(append).getOrElse(Nil)
        val y = e.value.map { v ⇒
          val h = e.key → v
          val t = e.nodes.map(append).getOrElse(Nil)
          h :: t
        }.getOrElse(Nil)

        x ++ y
      }
    }

    etcdClient.listDir(key.saneKey, recursive = true).map(r ⇒ r.node.nodes.map(append).getOrElse(Nil))
  }

  override def delete(key: String): Future[Unit] = etcdClient.deleteKey(key.saneKey).map(_ ⇒ ())
}
