package akka.contrib.pattern

import akka.actor.ActorSystem
import akka.testkit.TestKit

class EtcdClusterInventoryStoreTest extends TestKit(ActorSystem()) {
  val store = new EtcdInventoryStore("http://192.168.0.8:4001", system)

  ""
}
