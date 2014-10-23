package com.eigengo.lift.ml

import java.io.{File, FileOutputStream}

import scodec.bits.BitVector

object PebbleConverter extends App {
  
  val fileName = "training/chest1"

  // Do not touch
  import PebbleAccelerometerParser._
  val root = "/Users/janmachacek"
  val in = BitVector.fromInputStream(getClass.getResourceAsStream(s"/$fileName.dat"))
  val out = new FileOutputStream(new File(root, fileName.replace('/', '-') + ".csv"))
  parse(in).foreach(ad => ad.values.foreach(av => out.write(s"${av.x},${av.y},${av.z}\n".getBytes)))
  out.close()


}
