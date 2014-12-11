package net.nikore.etcd

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import net.nikore.etcd.EtcdExceptions._
import net.nikore.etcd.EtcdJsonProtocol._
import org.json4s.{DefaultFormats, Formats}
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.Json4sSupport
import spray.util._

import scala.concurrent.Future
import scala.concurrent.duration._

class EtcdClient(conn: String)(implicit system: ActorSystem) extends Json4sSupport {
  private val baseUrl = s"$conn/v2/keys"
  import system.dispatcher

  override implicit def json4sFormats: Formats = DefaultFormats

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
          throw KeyNotFoundException(e.message, "not found", e.index)
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

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}