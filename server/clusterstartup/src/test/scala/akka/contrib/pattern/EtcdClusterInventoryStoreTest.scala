package akka.contrib.pattern

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.Await

class EtcdClusterInventoryStoreTest extends TestKit(ActorSystem()) with FlatSpecLike with Matchers {
  import scala.concurrent.duration._
  val store = new EtcdInventoryStore("http://192.168.0.8:4001", system)
  val atMost = 3.seconds

  "etcd store" should "store stuff" in {
    store.set("system/foo/bar.baz/address", "some://complicated@string‡unicode")
    store.set("system/foo/bar.baz/status", "some://complicated@string‡unicode")
    store.set("system/foo/bar.baz/api", "some://complicated@string‡unicode")
    store.set("system/bar/bar.baz/api", "some://complicated@string‡unicode")

    Thread.sleep(3000)
    val all    = Await.result(store.getAll("system"), atMost)
    val values = Await.result(store.getAll("system/foo/bar.baz"), atMost)
    println(values)
    println(all)
  }
}
