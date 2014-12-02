package com.eigengo.lift.profile

import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps

object UserProfileMicroservice extends MicroserviceApp(MicroserviceProps("profile"))(UserProfileBoot.boot)
