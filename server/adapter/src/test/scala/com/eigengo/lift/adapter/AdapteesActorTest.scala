package com.eigengo.lift.adapter

import com.eigengo.lift.common.AdapterProtocol
import org.scalatest.{FlatSpec, Matchers}

class AdapteesActorTest extends FlatSpec with Matchers {

  "AdapteesActor companion" should "parse addresses" in {
    val Some(x) = AdapteesActor.Adaptee.unapply("xxx", "http://172.17.0.150:8080?version=1.0&side=q,c")
    x.key === "xxx"
    x.host === "127.17.0.150"
    x.port === 8080
    x.version === "1.0"
    x.side === Seq(AdapterProtocol.Command, AdapterProtocol.Query)
  }

}
