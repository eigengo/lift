package com.eigengo.lift.exercise

object RichArray {

  /**
   * Converts the ``NSData`` string output to array of bytes. An ``NSData`` log output
   * is usually displayed by running ``NSLog("%@", x)``, where ``x`` is an instance
   * of ``NSData``
   *
   * @param raw the NSData debug output—bytes grouped into 32bit words
   * @return the array of bytes
   */
  def formNSDataString(raw: String): Array[Byte] = {
    val s = raw.replaceAll(" ", "")
    (0 until s.length / 2).map { i ⇒
      val bs = s.substring(i * 2, i * 2 + 2)
      val b = Integer.parseInt(bs, 16).toByte
      b
    }.toArray
  }

}
