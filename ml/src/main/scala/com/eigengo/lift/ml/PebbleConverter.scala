package com.eigengo.lift.ml

import java.io.{File, FileOutputStream}

import scodec.bits.BitVector

object PebbleConverter extends App {
  
  val fileName = "accel-1413542383.400994"

  // Do not touch
  import PebbleAccelerometerParser._
  val root = new File(getClass.getResource("/").toURI)
  val in = BitVector.fromInputStream(getClass.getResourceAsStream(s"/$fileName.dat"))
  val out = new FileOutputStream(new File(root, fileName + ".csv"))
  parse(in).foreach(ad => ad.values.foreach(av => out.write(s"${av.x},${av.y},${av.z}\n".getBytes)))
  out.close()


}
