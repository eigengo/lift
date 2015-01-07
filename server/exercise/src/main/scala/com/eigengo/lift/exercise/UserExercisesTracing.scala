package com.eigengo.lift.exercise

import java.io.FileOutputStream

import scodec.bits.BitVector

import scala.util.Try

object UserExercisesTracing {

  def saveAccelerometerData(id: SessionId, datas: List[AccelerometerData]) = {
    Try {
      val fos = new FileOutputStream(s"/tmp/ad-$id.csv", true)
      datas.foreach { ad ⇒
        ad.values.foreach { v ⇒
          val line = s"${v.x},${v.y},${v.z}\n"
          fos.write(line.getBytes("UTF-8"))
        }
      }
      fos.close()
    }
  }


  def saveBits(id: SessionId, bits: BitVector): Unit = {
    Try { val fos = new FileOutputStream(s"/tmp/ad-$id.dat", true); fos.write(bits.toByteArray); fos.close() }
  }

}
