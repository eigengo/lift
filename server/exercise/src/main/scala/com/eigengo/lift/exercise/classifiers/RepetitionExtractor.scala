package com.eigengo.lift.exercise.classifiers

import java.io.FileOutputStream

import com.eigengo.lift.exercise.{AccelerometerValue, AccelerometerData}

object RepetitionExtractor {
  case class MovementTolerance(x: Int, y: Int, z: Int)

  private implicit class DoubleOps(d: Double) {
    private val epsilon = 0.001
    def !=~(that: Double) = Math.abs(d) < epsilon
  }

  private implicit class ListOps(l: List[Int]) {
    def smoothed(groupSize: Int): List[Int] = {
      l.grouped(groupSize).flatMap { group ⇒
        val x0 = group.head
        val dx = (group.last - x0) / group.size.toDouble
        val g = (0 until group.size).toList.map(i ⇒ (x0 + i * dx).toInt)
        g
      }.toList
    }

    def filterMovement(tolerance: Int): (Int, Int) = {
      val filtered = l.zipWithIndex.grouped(10).filter { group ⇒
        val (h, _) = group.head
        group.exists { case (x, idx) ⇒ Math.abs(h - x) > tolerance }
      }.toList.flatten

      if (filtered.isEmpty) 0 → l.size
      else {
        val (_, startIndex) = filtered.head
        val (_, endIndex) = filtered.last
        startIndex → endIndex
      }
    }

    def localExtremes(range: Int): List[LocalExtreme] = {
      val d = l.zipWithIndex.grouped(2).flatMap { group ⇒
        val (i, y0) = group.head
        val (_, y1) = group.last
        val dy = (y0 - y1) / 2.0
        List((i, dy))
      }
      val d2 = d.grouped(2).flatMap { group ⇒
        val (i, y0) = group.head
        val (_, y1) = group.last
        val dy = (y0 - y1) / 2.0
        if (dy > 0.0001) List(LocalMaximum(i, l(i)))
        else if (dy < 0.0001) List(LocalMinimum(i, l(i)))
        else List.empty
      }

      d2.toList
    }
  }

  private sealed trait LocalExtreme {
    def index: Int
    def value: Int
  }
  private case class LocalMinimum(index: Int, value: Int) extends LocalExtreme
  private case class LocalMaximum(index: Int, value: Int) extends LocalExtreme

  private sealed trait Axis
  private case object XAxis extends Axis
  private case object YAxis extends Axis
  private case object ZAxis extends Axis

  private case class PrincipalAxis(axis: Axis, values: List[Int]) {
    lazy val localExtremes = values.localExtremes(10)
  }

  private case class MovementModel(x: List[Int], y: List[Int], z: List[Int]) {
    require(x.size == y.size && y.size == z.size, "x, y, z must have the same number of elements")

    lazy val principalAxis: PrincipalAxis = {
      val rangeX = x.max - x.min
      val rangeY = y.max - y.min
      val rangeZ = z.max - z.min

      if (rangeX >= rangeY) {
        if (rangeX >= rangeZ) PrincipalAxis(XAxis, x) else PrincipalAxis(ZAxis, z)
      } else {
        if (rangeY >= rangeZ) PrincipalAxis(YAxis, y) else PrincipalAxis(ZAxis, z)
      }
    }

    def foreach[U](f: (Int, Int, Int) ⇒ U): Unit = {
      (0 until x.size).foreach { i ⇒
        f(x(i), y(i), z(i))
      }
    }
  }

  case class Repetition(avs: List[AccelerometerValue])
}

trait RepetitionExtractor {
  import RepetitionExtractor._

  private def filter(movementTolerance: MovementTolerance, avs: List[AccelerometerValue]): MovementModel = {
    val rawX = avs.map(_.x).smoothed(10)
    val rawY = avs.map(_.y).smoothed(10)
    val rawZ = avs.map(_.z).smoothed(10)
    val (sx, ex) = rawX.filterMovement(movementTolerance.x)
    val (sy, ey) = rawY.filterMovement(movementTolerance.y)
    val (sz, ez) = rawZ.filterMovement(movementTolerance.z)
    val s = List(sx, sy, sz).min
    val e = List(ex, ey, ez).max

    val range = (s until e).toList
    MovementModel(range.map(rawX.apply), range.map(rawY.apply), range.map(rawZ.apply))
  }

  def extract(movementTolerance: MovementTolerance)(ad: AccelerometerData): (AccelerometerData, List[Repetition]) = {
    val movement = filter(movementTolerance, ad.values)
    val pa = movement.principalAxis
    val pae = pa.localExtremes
    pa.values

    saveCsv("xyz", movement)

    (ad.copy(values = Nil), Nil)
  }

  // Eyeball debugging only ---------------------------------------------------------------------------------------------
  private def saveCsv(name: String, values: MovementModel): Unit = {
    val os = new FileOutputStream(s"/Users/janmachacek/$name.csv")
    values.foreach { case (x, y, z) ⇒
      val l = s"$x,$y,$z\n"
      os.write(l.getBytes("UTF-8"))
    }
    os.close()
  }

  private def saveCsv(name: String, values: List[Int]): Unit = {
    val os = new FileOutputStream(s"/Users/janmachacek/$name.csv")
    values.foreach { case x ⇒
      val l = s"$x\n"
      os.write(l.getBytes("UTF-8"))
    }
    os.close()
  }
  // Eyeball debugging only ---------------------------------------------------------------------------------------------

}

