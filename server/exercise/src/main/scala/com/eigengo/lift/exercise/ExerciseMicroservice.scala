package com.eigengo.lift.exercise

import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps
import com.eigengo.lift.profile.UserProfileLink

object ExerciseMicroservice extends MicroserviceApp(MicroserviceProps("exercise"))(ExerciseBoot.bootCluster)
