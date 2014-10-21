package com.eigengo.lift.ml

import java.io.{File, FileOutputStream}
import java.nio.file.FileSystem

import scodec.bits.BitVector

object PebbleConverter extends App {
  
  val fileName = "accel-1413884189.054500"

  // Do not touch
  import PebbleAccelerometerParser._
  val root = "/Users/janmachacek"
  val in = BitVector.fromInputStream(getClass.getResourceAsStream(s"/$fileName.dat"))
  val out = new FileOutputStream(new File(root, fileName + ".csv"))
  parse(in).foreach(ad => ad.values.foreach(av => out.write(s"${av.x},${av.y},${av.z}\n".getBytes)))
  out.close()


}
