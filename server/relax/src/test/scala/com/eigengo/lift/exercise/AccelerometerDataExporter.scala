package com.eigengo.lift.exercise

import java.io.FileOutputStream

object AccelerometerDataExporter {

  def exportToCsv(ads: List[AccelerometerData], name: String, append: Boolean = true): Unit = {
    val homeDir = System.getProperty("user.home")
    val os = new FileOutputStream(s"$homeDir/$name.csv", append)
    ads.foreach(_.values.foreach { av ⇒
      val s = s"${av.x},${av.y},${av.z}\n"
      os.write(s.getBytes("UTF-8"))
    })
    os.close()
  }

}
