package com.eigengo.lift.ml

import java.io.{InputStreamReader, File, FileInputStream}

import scodec.bits.BitVector

import scala.io.Source

object PebbleAccelerometerParserTest extends App {
  // f7fe 0300 64 -> count = 3, 100 Hz
  // ffff ffff 01 ->  -1,   -1,   -1
  // 0108 4000 00 ->   1,    1,    1
  // 8cc0 39fa 00 -> 140, -200, 1000

  import PebbleAccelerometerParser._

  val three = BitVector(0xf7, 0xff, 0x03, 0x00, 0x64,
                        0xff, 0xff, 0xff, 0xff, 0x01,
                        0x01, 0x08, 0x40, 0x00, 0x00,
                        0x8c, 0xc0, 0x39, 0xfA, 0x00)

  println(parse(three))

  println(parsePackedAccelerometerData(BitVector(0xFF, 0xFF, 0xFF, 0xFF, 0x01)))
  println(parsePackedAccelerometerData(BitVector(0x01, 0x08, 0x40, 0x00, 0x00)))
  println(parsePackedAccelerometerData(BitVector(0x8C, 0xC0, 0x39, 0xFA, 0x00)))


}
