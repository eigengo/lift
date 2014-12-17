package akka.contrib.pattern

import akka.actor._

import scala.util.{Failure, Success}

object NodeManager {

  /**
   * The connector API, adapts the health information for the clients to use. For example,
   * the Spray Connector exposes the status as a ``Route``.
   */
  trait Connector extends akka.actor.Extension {

    /**
     * The connector type
     */
    type Conn

    /**
     * Returns the connector of the given type
     * @return the instance of the connector
     */
    def connector: Conn
  }

  /**
   * The node manager extension
   */
  private[akka] trait Manager extends akka.actor.Extension {
    def stop(): Unit

    def kill(): Unit
  }


  /**
   * Returns the ``connector`` value pulled from appropriate the ``Connector`` extension.
   * @param key the extension key
   * @param system the ActorSystem with the extension
   * @tparam C the type of the Connector
   * @return the instance of ``C#Conn``: the concrete value of type the connector provides
   */
  def apply[C <: Connector](key: ExtensionId[C])(implicit system: ActorSystem): C#Conn = {
    key(system).connector
  }

  private[akka] def manager(implicit system: ActorSystem): Manager = Manager(system)

  private[akka] object Manager extends ExtensionKey[ManagerExt]

  private[akka] class ManagerExt(system: ExtendedActorSystem) extends Manager {
    import akka.pattern.gracefulStop
    import scala.concurrent.duration._

    private def stopOrKill[U](exit: Int ⇒ U): Unit = {
      import system.dispatcher

      gracefulStop(system.guardian, 50.seconds).onComplete {
        case Success(_) ⇒
          system.shutdown()
          exit(0)
        case Failure(_) ⇒
          system.shutdown()
          exit(127)
      }
    }

    override def stop(): Unit = stopOrKill(_ ⇒ ())

    override def kill(): Unit = stopOrKill(sys.exit(_))
  }
}

