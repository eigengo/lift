package com.eigengo.lift.exercise.classifiers

import java.io.FileOutputStream

import com.eigengo.lift.exercise.{AccelerometerData, AccelerometerValue}

object RepetitionExtractor {
  case class MovementTolerance(x: Double, y: Double, z: Double)

  /**
   * Double operations
   * @param d the double
   */
  private implicit class DoubleOps(d: Double) {
    private val epsilon = 0.001
    def !=~(that: Double) = Math.abs(d) < epsilon
  }

  /**
   * Add calculus-like methods to List[Int]
   * @param list the underlying list
   */
  private implicit class ListCalculus(list: List[Int]) {

    /**
     * Removes noise from the values
     * @param windowSize the averaging window
     * @return the smoothed list
     */
    def smoothed(windowSize: Int): List[Int] = {
      list.grouped(windowSize).flatMap { group ⇒
        val x0 = group.head
        val dx = (group.last - x0) / group.size.toDouble
        val g = (0 until group.size).toList.map(i ⇒ (x0 + i * dx).toInt)
        g
      }.toList
    }

    /**
     * The average of the values in this list
     */
    lazy val avg: Double = list.sum / list.size

    /**
     * The range of the values in this list
     */
    lazy val range: Int = list.max - list.min

    /**
     * Computes the trim range, when trimmed, the list contains only "useful" values
     * @param epsilon the tolerance in fraction of entire range; typically 0.1
     * @return the ranges to be removed
     */
    def trimRange(epsilon: Double): (Int, Int) = {
      val tolerance = epsilon * 2048
      val filtered = list.zipWithIndex.grouped(10).filter { group ⇒
        val (h, _) = group.head
        group.exists { case (x, idx) ⇒ Math.abs(h - x) > tolerance }
      }.toList.flatten

      if (filtered.isEmpty) 0 → list.size
      else {
        val (_, startIndex) = filtered.head
        val (_, endIndex) = filtered.last
        startIndex → endIndex
      }
    }

    /**
     * Computes the local minima and maxima
     *
     * @param windowSize the window size to prevent noise
     * @return the local extremes
     */
    def localExtremes(windowSize: Int): List[LocalExtreme] = {
      val epsilon = 0.1
      // diff(...)
      val d = list.zipWithIndex.grouped(windowSize).flatMap { group ⇒
        val values = group.map(_._1)
        val y0 = values.slice(0, windowSize / 2).avg
        val y1 = values.slice(windowSize / 2, windowSize - 1).avg
        val i = group(windowSize / 2)._2
        val dy = (y0 - y1) / 2.0
        List((i, dy))
      }.toList

      // inflection points
      val x = d.grouped(2).flatMap { x ⇒
        val (i, d0) = x.head
        val (_, d1) = x.last

        if (d0 > 0 && d1 < 0) {
          List(LocalMaximum(i, list(i)))
        } else if (d0 < 0 && d1 > 0) {
          List(LocalMinimum(i, list(i)))
        } else List.empty
      }.toList

      // remove extremes that are too close to each other
      val r = x.grouped(2).flatMap { g ⇒
        val m0 = g.head
        val m1 = g.last
        if (Math.abs(m0.value - m1.value) > range * epsilon) List(m0, m1) else List.empty
      }.toList

      if (!r.isEmpty) {
        val f = if (list.head > r.head.value) LocalMaximum(0, list.head) else LocalMinimum(0, list.head)
        val l = if (list.last > r.head.value) LocalMaximum(list.size - 1, list.last) else LocalMinimum(list.size - 1, list.last)
        f :: (r :+ l)
      } else List.empty
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

  private case class MovementModel(x: List[Int], y: List[Int], z: List[Int]) extends Iterable[(Int, Int, Int)] {
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

    override def iterator: Iterator[(Int, Int, Int)] =
      (0 until x.size).map { i ⇒
        (x(i), y(i), z(i))
      }.iterator

  }

  case class Repetition(avs: List[AccelerometerValue])
}

trait RepetitionExtractor {
  import com.eigengo.lift.exercise.classifiers.RepetitionExtractor._

  private def filter(movementTolerance: MovementTolerance, avs: List[AccelerometerValue]): MovementModel = {
    val rawX = avs.map(_.x).smoothed(10)
    val rawY = avs.map(_.y).smoothed(10)
    val rawZ = avs.map(_.z).smoothed(10)
    val (sx, ex) = rawX.trimRange(movementTolerance.x)
    val (sy, ey) = rawY.trimRange(movementTolerance.y)
    val (sz, ez) = rawZ.trimRange(movementTolerance.z)
    val s = List(sx, sy, sz).min
    val e = List(ex, ey, ez).max

    val range = (s until e).toList
    MovementModel(range.map(rawX.apply), range.map(rawY.apply), range.map(rawZ.apply))
  }

  def extract(movementTolerance: MovementTolerance)(ad: AccelerometerData): (AccelerometerData, List[Repetition]) = {
    val movement = filter(movementTolerance, ad.values)
    val pa = movement.principalAxis
    val pae = pa.localExtremes

    saveCsv("xyz", movement) { case (x, y, z) ⇒ s"$x,$y,$z" }
    saveCsv("pa", pa.values) { _.toString }
    saveCsv("pae", pae) { le: LocalExtreme ⇒ s"${le.index}" }

    (ad.copy(values = Nil), Nil)
  }

  // Eyeball debugging only ---------------------------------------------------------------------------------------------

  private def saveCsv[A](name: String, values: Iterable[A])(f: A ⇒ String): Unit = {
    import scala.language.reflectiveCalls

    val os = new FileOutputStream(s"/Users/janmachacek/$name.csv")
    values.iterator.foreach { case x ⇒
      val l = s"${f(x)}\n"
      os.write(l.getBytes("UTF-8"))
    }
    os.close()
  }
  // Eyeball debugging only ---------------------------------------------------------------------------------------------

}

