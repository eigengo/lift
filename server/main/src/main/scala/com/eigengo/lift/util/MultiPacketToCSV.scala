package com.eigengo.lift.util

import com.eigengo.lift.exercise._
import java.io._
import scodec.bits.BitVector

/**
 * Simple utility application for reading in raw binary accelerometer data, decoding it and then dumping it to a CSV file.
 */
object MultiPacketToCSV extends App {

  if (args.length != 2) {
    println("Usage: MultiPacketToCSV <input raw binary filename> <output CSV filename>")
    sys.exit(1)
  }

  val inFileName = args(0)
  val outFileName = args(1)

  val decoderData = BitVector.fromMmap(new FileInputStream(new File(inFileName)).getChannel)

  val fd = new FileWriter(outFileName, true)
  try {
    fd.write("\"location\",\"rate\",\"x\",\"y\",\"z\"\n")
    for (block <- MultiPacketDecoder.decode(decoderData.toByteBuffer)) {
      for (pkt <- block.packets) {
        for (data <- RootSensorDataDecoder(AccelerometerDataDecoder).decodeAll(pkt.payload)) {
          val csv = data.asInstanceOf[List[AccelerometerData]].flatMap { d => d.values.map(v => s"${pkt.sourceLocation},${d.samplingRate},${v.x},${v.y},${v.z}")}.mkString("", "\n", "\n")

          fd.write(csv)
        }
      }
    }
  } finally {
    fd.close()
  }

}
