package com.eigengo.lift.exercise.classifiers

import com.eigengo.lift.exercise.AccelerometerData
import com.eigengo.lift.exercise.classifiers.RepetitionExtractor.MovementTolerance
import org.scalatest.{Matchers, FlatSpec}
import scodec.bits.BitVector

class RepetitionExtractorTest extends FlatSpec with Matchers with RepetitionExtractor {

  private def loadAd(name: String): AccelerometerData ={
    val bv = BitVector.fromInputStream(getClass.getResourceAsStream(name))
    val (BitVector.empty, ads) = AccelerometerData.decodeAll(bv, Nil)
    ads.tail.foldLeft(ads.head)((r, ad) ⇒ r.copy(values = r.values ++ ad.values))
  }

  "RepetitionExtractor" should "extract reps" in {
    val ad = loadAd("/measured/bicep-1/all.dat")
    val pebbleMovementTolerance = MovementTolerance(80 / 2048.0, 80 / 2048.0, 160 / 2048.0)
    extract(pebbleMovementTolerance)(ad)

    true
  }

}
