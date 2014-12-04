package com.eigengo.lift.profile

import java.util.UUID

import akka.actor.ActorRef
import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import com.eigengo.lift.profile.UserProfileProcessor.{UserLogin, UserRegister, UserSetDevice, UserSetPublicProfile}
import com.eigengo.lift.profile.UserProfileProtocol._
import spray.routing.Directives

import scala.concurrent.ExecutionContext

trait ProfileService extends Directives with CommonMarshallers with CommonPathDirectives {
  import akka.pattern.ask
  import com.eigengo.lift.common.Timeouts.defaults._

  def userProfileRoute(userProfile: ActorRef, userProfileProcessor: ActorRef)(implicit ec: ExecutionContext) =
    path("user") {
      post {
        handleWith { register: UserRegister ⇒
          (userProfileProcessor ? register).mapRight[UUID]
        }
      } ~
      put {
        handleWith { login: UserLogin ⇒
          (userProfileProcessor ? login).mapRight[UUID]
        }
      }
    } ~
    path("user" / UserIdValue) { userId ⇒
      get {
        complete {
          (userProfile ? UserGetPublicProfile(userId)).mapNoneToEmpty[PublicProfile]
        }
      } ~
      post {
        handleWith { publicProfile: PublicProfile ⇒
          (userProfileProcessor ? UserSetPublicProfile(userId, publicProfile)).mapRight[Unit]
        }
      }
    } ~
    path("user" / UserIdValue / "device" / "ios") { userId ⇒
      post {
        handleWith { device: IOSUserDevice ⇒
          (userProfileProcessor ? UserSetDevice(userId, device)).mapRight[Unit]
        }
      }
    } ~
    path("user" / UserIdValue / "device" / "android") { userId ⇒
      post {
        handleWith { device: AndroidUserDevice ⇒
          (userProfileProcessor ? UserSetDevice(userId, device)).mapRight[Unit]
        }
      }
    }

}
