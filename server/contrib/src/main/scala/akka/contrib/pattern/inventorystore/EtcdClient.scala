package akka.contrib.pattern.inventorystore

import akka.actor.ActorSystem
import org.json4s.{DefaultFormats, Formats}
import spray.client.pipelining._
import spray.http._
import spray.httpx.Json4sSupport

import scala.concurrent.Future

private[akka] object EtcdClient {

  //single key/values
  case class NodeResponse(key: String, value: Option[String], modifiedIndex: Int, createdIndex: Int)
  case class EtcdResponse(action: String, node: NodeResponse, prevNode: Option[NodeResponse])

  //for hanlding dirs
  case class NodeListElement(key: String, dir: Option[Boolean], value: Option[String], nodes: Option[List[NodeListElement]])
  case class EtcdListResponse(action: String, node: NodeListElement)

  //for handling error messages
  case class Error(errorCode: Int, message: String, cause: String, index: Int)
}


private[akka] class EtcdClient(conn: String)(implicit system: ActorSystem) extends Json4sSupport {
  import EtcdClient._
  private val baseUrl = s"$conn/v2/keys"
  import system.dispatcher
  override implicit val json4sFormats: Formats = DefaultFormats

  def getKey(key: String): Future[EtcdResponse] = {
    getKeyAndWait(key, wait = false)
  }

  def getKeyAndWait(key: String, wait: Boolean = true): Future[EtcdResponse] = {
    defaultPipeline(Get(s"$baseUrl/$key?wait=$wait"))
  }

  def setKey(key: String, value: String): Future[EtcdResponse] = {
    defaultPipeline(Put(Uri(s"$baseUrl/$key").withQuery(Map("value" -> value))))
  }

  def deleteKey(key: String): Future[EtcdResponse] = {
    defaultPipeline(Delete(s"$baseUrl/$key"))
  }

  def createDir(dir: String): Future[EtcdResponse] = {
    defaultPipeline(Put(s"$baseUrl/$dir?dir=true"))
  }

  def listDir(dir: String, recursive: Boolean = false): Future[EtcdListResponse] = {
    val pipline: HttpRequest => Future[EtcdListResponse] = (
      sendReceive
        ~> mapErrors
        ~> unmarshal[EtcdListResponse]
      )

    pipline(Get(s"$baseUrl/$dir/?recursive=$recursive"))
  }

  def deleteDir(dir: String, recursive: Boolean = false): Future[EtcdResponse] = {
    defaultPipeline(Delete(s"$baseUrl/$dir?recursive=$recursive"))
  }

  private val mapErrors = (response: HttpResponse) => {
    if (response.status.isSuccess) response
    else {
      serialization.read[Error](response.entity.asString) match {
        case e if e.errorCode == 100 ⇒
          throw new NoSuchKeyException(e.message)
        case e ⇒
          throw new RuntimeException("General error: " + e.toString)
      }
    }
  }

  private val defaultPipeline: HttpRequest => Future[EtcdResponse] = (
    sendReceive
      ~> mapErrors
      ~> unmarshal[EtcdResponse]
    )

}
