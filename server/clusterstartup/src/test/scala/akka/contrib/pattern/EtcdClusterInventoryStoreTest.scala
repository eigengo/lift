package akka.contrib.pattern

import akka.actor.{AddressFromURIString, ActorSystem}
import akka.testkit.TestKit
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.Await
import scala.util.{Success, Failure}

class EtcdClusterInventoryStoreTest extends TestKit(ActorSystem()) with FlatSpecLike with Matchers {
  import scala.concurrent.duration._
  val store = new EtcdInventoryStore("http://192.168.59.103:4001", system)
  val atMost = 3.seconds
/*
  "etcd store" should "store stuff" in {
    store.set("system/foo/bar.baz/address", "some://complicated@string‡unicode")
    store.set("system/foo/bar.baz/status", "some://complicated@string‡unicode")
    store.set("system/foo/bar.baz/api", "some://complicated@string‡unicode")
    store.set("system/bar/bar.baz/api", "some://complicated@string‡unicode")
    store.set("system/frontent.cluster.nodes/node/akka.tcp_192.168.0.8_2552", "akka.tcp://Lift@192.168.0.8:2552")

    Thread.sleep(3000)
    val all    = Await.result(store.getAll("system"), atMost)
    val values = Await.result(store.getAll("system/foo/bar.baz"), atMost)
    println(values)
    println(all)
  }
*/
  "etcd store" should "delete stuff" in {
    import system.dispatcher

    val x = Await.ready(store.delete("/system/frontent.cluster.nodes/api/akka.tcp_172.17.0.185_2552"), atMost)
    x.value match {
      case Some(Success(x)) ⇒ println("OK")
      case Some(Failure(t)) ⇒ t.printStackTrace()
      case None ⇒ println(">???")
    }
  }
}
