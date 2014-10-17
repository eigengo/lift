package com.eigengo.lift.ml

object PebbleAccelerometerParser {

  case class AccelerometerData(x: Int, y: Int, z: Int)

}

class PebbleAccelerometerParser {
  import PebbleAccelerometerParser._

  def parse(bytes: Array[Byte]): AccelerometerData = {
    // FF, FF, FF, FF, 01 is pad->x_val = -1; pad->y_val = -1; pad->z_val = -1;
    // 01, 08, 40, 00, 00 is pad->x_val = +1; pad->y_val = +1; pad->z_val = +1;

    // 8C, C0, 39, FA, 00 is pad->x_val = 140; pad->y_val = -200; pad->z_val = 1000;


    // x  8B from (0): 0-7 + 3B from (1): 0-2
    // y  5B from (1): 3-7 + 6B from (2): 0-5
    // z  2B from (2): 6-7 + 8B from (3): 0-7 + 1B from (3) 0

    val x: Int = bytes(0) + bytes(1) & 3
    val y = bytes(1)
  }

}
