package com.eigengo.lift.exercise

import com.eigengo.lift.common.MicroserviceApp

object ExerciseMicroservice extends MicroserviceApp("exercise")(ExerciseBoot.boot)
