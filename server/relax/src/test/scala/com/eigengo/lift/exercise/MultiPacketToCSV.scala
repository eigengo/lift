package com.eigengo.lift.exercise

import java.io._

import scodec.bits.BitVector

/**
 * Simple utility application for reading in raw binary multi-packet data, decoding it and then dumping it to a CSV file.
 * Currently, we only support decoding of accelerometer data.
 *
 * To use this utility, type the following at an sbt prompt:
 * ```
 *   project main
 *   runMain com.eigengo.lift.util.MultiPacketToCSV /path/to/fe2a035a-c0d0-47e5-8bce-370759e53885.mp /path/to/fe2a035a-c0d0-47e5-8bce-370759e53885.csv
 * ```
 */
object MultiPacketToCSV extends App {

  if (args.length != 2) {
    println("Usage: MultiPacketToCSV <input raw binary filename> <output CSV filename>")
    sys.exit(1)
  }

  val inFileName = args(0)
  val outFileName = args(1)

  // List of decoders that this utility supports
  val decoderSupport = Seq(
    AccelerometerDataDecoder
  )

  val decoderData = BitVector.fromMmap(new FileInputStream(new File(inFileName)).getChannel)

  val fd = new FileWriter(outFileName, true)
  try {
    fd.write("\"timestamp\",\"location\",\"rate\",\"x\",\"y\",\"z\"\n")
    for (block <- MultiPacketDecoder.decode(decoderData.toByteBuffer)) {
      for (pkt <- block.packets) {
        for (data <- RootSensorDataDecoder(decoderSupport: _*).decodeAll(pkt.payload)) {
          val csv = data.asInstanceOf[List[AccelerometerData]].flatMap { d => d.values.map(v => s"${block.packets},${pkt.sourceLocation},${d.samplingRate},${v.x},${v.y},${v.z}")}.mkString("", "\n", "\n")

          fd.write(csv)
        }
      }
    }
  } finally {
    fd.close()
  }

}
