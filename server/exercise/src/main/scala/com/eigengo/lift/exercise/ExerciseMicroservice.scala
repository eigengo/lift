package com.eigengo.lift.exercise

import com.eigengo.lift.common.MicroserviceApp
import com.eigengo.lift.common.MicroserviceApp.MicroserviceProps

object ExerciseMicroservice extends MicroserviceApp(MicroserviceProps("exercise"))(ExerciseBoot.boot)
