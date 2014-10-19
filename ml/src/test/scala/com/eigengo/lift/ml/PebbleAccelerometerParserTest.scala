package com.eigengo.lift.ml

import java.io.{InputStreamReader, File, FileInputStream}

import scodec.bits.BitVector

import scala.io.Source

object PebbleAccelerometerParserTest extends App {
  // FF, FF, FF, FF, 01 is pad->x_val = -1; pad->y_val = -1; pad->z_val = -1;
  // 01, 08, 40, 00, 00 is pad->x_val = +1; pad->y_val = +1; pad->z_val = +1;
  // 8C, C0, 39, FA, 00 is pad->x_val = 140; pad->y_val = -200; pad->z_val = 1000;

  import PebbleAccelerometerParser._

  println(parsePackedAccelerometerData(BitVector(0xFF, 0xFF, 0xFF, 0xFF, 0x01)))
  println(parsePackedAccelerometerData(BitVector(0x01, 0x08, 0x40, 0x00, 0x00)))
  println(parsePackedAccelerometerData(BitVector(0x8C, 0xC0, 0x39, 0xFA, 0x00)))


}
