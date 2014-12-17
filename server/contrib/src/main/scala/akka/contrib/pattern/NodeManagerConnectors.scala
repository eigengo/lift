package akka.contrib.pattern

import java.lang.management.ManagementFactory
import javax.management.ObjectName

import akka.actor.{ExtendedActorSystem, ExtensionKey}
import spray.http.{StatusCodes, HttpResponse}
import spray.routing._

object JmxConnector extends ExtensionKey[JmxConnectorExt]

object JmxConnectorExt {

  /**
   * MXBean interface for the ``ServiceStatus`` case class.
   */
  trait ClusterNodeMXBean {
    def stop(): Unit

    def kill(): Unit
  }

}

class JmxConnectorExt(system: ExtendedActorSystem) extends NodeManager.Connector {
  import JmxConnectorExt._
  type Conn = String ⇒ Unit
  private val mbeanServer = ManagementFactory.getPlatformMBeanServer

  override val connector: Conn = { microserviceName ⇒
    val objectName = new ObjectName(s"com.eigengo.lift:microservice=$microserviceName,type=NodeManager,name=Control")
    val mbean = new ClusterNodeMXBean {
      override def stop(): Unit = NodeManager.manager(system).stop()

      override def kill(): Unit = NodeManager.manager(system).kill()
    }
    mbeanServer.registerMBean(mbean, objectName)
  }
}

object SprayConnector extends ExtensionKey[SprayConnectorExt]

class SprayConnectorExt(system: ExtendedActorSystem) extends NodeManager.Connector with Directives {
  type Conn = String ⇒ Route

  private def stopOrKill(shutdownToken: String, f: () ⇒ Unit): Route = {
    delete {
      parameter('token) { token ⇒
        complete {
          if (token == shutdownToken)
            HttpResponse(entity = "{}")
          else {
            f()
            HttpResponse(status = StatusCodes.Forbidden, entity = "Bad shutdown token")
          }
        }
      }
    }
  }

  override val connector: Conn = { shutdownToken ⇒
    path("system" / "stop") {
      stopOrKill(shutdownToken, NodeManager.manager(system).stop)
    } ~
    path("system" / "kill") {
      stopOrKill(shutdownToken, NodeManager.manager(system).kill)
    }
  }
}