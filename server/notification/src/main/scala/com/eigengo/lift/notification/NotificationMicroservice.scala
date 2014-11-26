package com.eigengo.lift.notification

import com.eigengo.lift.common.MicroserviceApp

object NotificationMicroservice extends MicroserviceApp("notification")(NotificaitonBoot.boot)
