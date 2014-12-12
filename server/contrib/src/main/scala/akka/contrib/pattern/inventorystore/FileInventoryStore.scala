package akka.contrib.pattern.inventorystore

import scala.concurrent.Future
import scala.io.Source
import scala.util.Try

private[akka] object FileInventoryStore {

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

private[akka] class FileInventoryStore(dir: String) extends InventoryStore {
  import java.io.{File, FileOutputStream}
  import FileInventoryStore._

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

