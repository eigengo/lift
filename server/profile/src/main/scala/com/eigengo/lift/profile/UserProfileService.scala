package com.eigengo.lift.profile

import akka.actor.ActorRef
import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import com.eigengo.lift.profile.UserProfileProcessor.UserRegister
import com.eigengo.lift.profile.UserProfileProtocol.{AndroidUserDevice, IOSUserDevice, UserSetDevice, UserDevice}
import spray.routing.Directives

import scala.concurrent.ExecutionContext

trait UserProfileService extends Directives with CommonMarshallers with CommonPathDirectives {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def userProfileRoute(userProfile: ActorRef, userProfileProcessor: ActorRef)(implicit ec: ExecutionContext) =
    path("user") {
      post {
        handleWith { register: UserRegister ⇒
          (userProfileProcessor ? register).map(_.toString)
        }
      }
    } ~
    path("user" / UserIdValue / "device" / "ios") { userId ⇒
      post {
        handleWith { device: IOSUserDevice ⇒
          (userProfile ? UserSetDevice(userId, device)).map(_.toString)
        }
      }
    } ~
    path("user" / UserIdValue / "device" / "android") { userId ⇒
      post {
        handleWith { device: AndroidUserDevice ⇒
          (userProfile ? UserSetDevice(userId, device)).map(_.toString)
        }
      }
    }

}
