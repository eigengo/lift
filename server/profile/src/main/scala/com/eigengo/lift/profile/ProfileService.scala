package com.eigengo.lift.profile

import java.util.UUID

import akka.actor.ActorRef
import com.eigengo.lift.common.{CommonMarshallers, CommonPathDirectives}
import com.eigengo.lift.notification.NotificationProtocol.{AndroidDevice, IOSDevice}
import com.eigengo.lift.profile.UserProfileProcessor._
import com.eigengo.lift.profile.UserProfileProtocol._
import spray.http._
import spray.routing.Directives

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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
    path("user" / UserIdValue / "check") { userId ⇒
      get {
        complete {
          (userProfileProcessor ? UserCheckAccount(userId)).mapTo[Boolean].map { x ⇒
            if (x) HttpResponse(StatusCodes.OK) else HttpResponse(StatusCodes.NotFound)
          }
        }
      }
    } ~
    path("user" / UserIdValue / "image") { userId ⇒
      get {
        complete {
          (userProfile ? UserGetProfileImage(userId)).mapTo[Option[Array[Byte]]].map { x ⇒
            HttpResponse(entity = HttpEntity(contentType = ContentType(MediaTypes.`image/png`), bytes = x.getOrElse(Array.empty)))
          }
        }
      } ~
      post {
        ctx ⇒
          val image = ctx.request.entity.data.toByteArray
          (userProfileProcessor ? UserSetProfileImage(userId, image)).onComplete {
            case Success(_) ⇒ ctx.complete(HttpResponse(StatusCodes.OK))
            case Failure(_) ⇒ ctx.complete(HttpResponse(StatusCodes.InternalServerError))
          }
      }
    } ~
    path("user" / UserIdValue / "device" / "ios") { userId ⇒
      post {
        handleWith { device: IOSDevice ⇒
          (userProfileProcessor ? UserSetDevice(userId, device)).mapRight[Unit]
        }
      }
    } ~
    path("user" / UserIdValue / "device" / "android") { userId ⇒
      post {
        handleWith { device: AndroidDevice ⇒
          (userProfileProcessor ? UserSetDevice(userId, device)).mapRight[Unit]
        }
      }
    }

}
