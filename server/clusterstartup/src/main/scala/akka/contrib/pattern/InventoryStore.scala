package akka.contrib.pattern

import akka.actor.ActorSystem
import com.typesafe.config.Config
import net.nikore.etcd.{EtcdJsonProtocol, EtcdClient}

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

case class NoSuchKeyException(key: String) extends RuntimeException(s"No such key $key")

object InventoryStore {
  def apply(config: Config, system: ActorSystem): InventoryStore = config.getString("plugin") match {
      case "file" ⇒ new FileInventoryStore(config.getString("file.fileName"))
      case "etcd" ⇒ new EtcdInventoryStore(config.getString("etcd.url"), system)
  }
}

trait InventoryStore {

  def get(key: String): Future[String]

  def getAll(key: String): Future[List[(String, String)]]

  def set(key: String, node: String): Future[Unit]

  def delete(key: String): Future[Unit]

}

object FileInventoryStore {

  case class KISN(key: String, value: String)

  private def parseKisn(line: String): Option[KISN] = {
    val elems = line.split("⇒")
    if (elems.length == 2) {
      Some(KISN(elems(0), elems(1)))
    } else None
  }

  private def kisnToString(kisn: KISN): String = {
    s"${kisn.key}⇒${kisn.value}"
  }

  implicit object KVOrdering extends Ordering[KISN] {
    override def compare(x: KISN, y: KISN): Int = x.key.compareTo(y.key)
  }
}

class FileInventoryStore(dir: String) extends InventoryStore {
  import java.io.{File, FileOutputStream}

import akka.contrib.pattern.FileInventoryStore._
  private val inventory = new File(dir + "all")

  private def load(): List[KISN] = {
    Try {
      val lines = Source.fromFile(inventory).getLines().toList
      lines.flatMap(parseKisn).sorted
    }.getOrElse(Nil)
  }

  private def save(kisns: List[KISN]): Unit = {
    val bytes = kisns.sorted.map(kisnToString).mkString("\n").getBytes("UTF-8")
    val fos = new FileOutputStream(inventory, false)
    fos.write(bytes)
    fos.close()
  }

  override def get(key: String): Future[String] = {
    load().find(_.key == key).fold[Future[String]](Future.failed(NoSuchKeyException(key)))(x ⇒ Future.successful(x.value))
  }

  override def set(key: String, value: String): Future[Unit] = {
    val kisns = KISN(key, value) :: load().dropWhile(_.key == key)
    Future.successful(save(kisns))
  }

  override def getAll(key: String): Future[List[(String, String)]] = {
    Future.successful(load().filter(_.key.startsWith(key)).map(kisn ⇒ kisn.key → kisn.value))
  }

  override def delete(key: String): Future[Unit] = {
    Future.successful(())
  }
}

class EtcdInventoryStore(url: String, system: ActorSystem) extends InventoryStore {
  val etcdClient = new EtcdClient(url)(system)
  import system.dispatcher

  override def get(key: String): Future[String] = {
    etcdClient.getKey(key).map(r ⇒ r.node.value.get)
  }

  override def set(key: String, value: String): Future[Unit] = {
    (if (key.contains("/")) {
      val i = key.lastIndexOf('/')
      val directory = key.substring(0, i)
      etcdClient.createDir(directory).zip(etcdClient.setKey(key, value)).map(_ ⇒ ())
    }
    else etcdClient.setKey(key, value)).map(_ ⇒ ())
  }

  override def getAll(key: String): Future[List[(String, String)]] = {
    def append(nodes: List[EtcdJsonProtocol.NodeListElement]): List[(String, String)] = {
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

    etcdClient.listDir(key, recursive = true).map(r ⇒ r.node.nodes.map(append).getOrElse(Nil))
  }

  override def delete(key: String): Future[Unit] = etcdClient.deleteKey(key).map(_ ⇒ ())
}
