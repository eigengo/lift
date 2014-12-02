package com.eigengo.lift.notification

import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps

object NotificationMicroservice extends MicroserviceApp(MicroserviceProps("notification"))(NotificationBoot.boot)
