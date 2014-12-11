package akka.contrib.pattern

import akka.actor.ActorSystem
import com.typesafe.config.Config
import net.nikore.etcd.EtcdClient

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

case class NoSuchKeyException(key: String) extends RuntimeException(s"No such key $key")

case class InventoryStoreNodeAttribute(name: String, value: String)
case class InventoryStoreNode(value: String, attributes: Seq[InventoryStoreNodeAttribute])

object InventoryStore {
  def apply(config: Config, system: ActorSystem): InventoryStore = config.getString("plugin") match {
      case "file" ⇒ new FileInventoryStore(config.getString("file.fileName"))
      case "etcd" ⇒ new EtcdInventoryStore(config.getString("etcd.url"), system)
  }
}

trait InventoryStore {

  def get(key: String): Future[InventoryStoreNode]

  def getAll(key: String): Future[List[InventoryStoreNode]]

  def set(key: String, node: InventoryStoreNode): Future[Unit]

  def delete(key: String): Future[Unit]

}

object FileInventoryStore {

  case class KISN(key: String, isn: InventoryStoreNode)

  private def parseKisn(line: String): Option[KISN] = {
    val elems = line.split("⇒")
    if (elems.length == 2) {
      Some(KISN(elems(0), InventoryStoreNode(elems(1), Nil)))
    } else None
  }

  private def kisnToString(kisn: KISN): String = {
    s"${kisn.key}⇒${kisn.isn.value}"
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

  override def get(key: String): Future[InventoryStoreNode] = {
    load().find(_.key == key).fold[Future[InventoryStoreNode]](Future.failed(NoSuchKeyException(key)))(x ⇒ Future.successful(x.isn))
  }

  override def set(key: String, node: InventoryStoreNode): Future[Unit] = {
    val kisns = KISN(key, node) :: load().dropWhile(_.key == key)
    Future.successful(save(kisns))
  }

  override def getAll(key: String): Future[List[InventoryStoreNode]] = {
    Future.successful(load().filter(_.key.startsWith(key)).map(_.isn))
  }

  override def delete(key: String): Future[Unit] = {
    Future.successful(())
  }
}

class EtcdInventoryStore(url: String, system: ActorSystem) extends InventoryStore {
  val etcdClient = new EtcdClient(url)(system)
  import system.dispatcher

  override def get(key: String): Future[InventoryStoreNode] = etcdClient.getKey(key).map(r ⇒ InventoryStoreNode(r.node.value.get, Nil))

  override def set(key: String, node: InventoryStoreNode): Future[Unit] = etcdClient.setKey(key, node.value).map(_ ⇒ ())

  override def getAll(key: String): Future[List[InventoryStoreNode]] = etcdClient.listDir(key).map(r ⇒ r.node.nodes.map { nle ⇒
    nle.map(e ⇒ InventoryStoreNode(e.value.get, Nil))
  }.getOrElse(Nil))

  override def delete(key: String): Future[Unit] = etcdClient.deleteKey(key).map(_ ⇒ ())
}
