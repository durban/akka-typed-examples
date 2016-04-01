package com.example.shless

import shapeless._

object TypeableExample {

  final case class C[A](s: String)(x: A) {
    val y: A = x
  }

  val tt = Typeable[C[Int]]

  val v1: Any = C[Int]("a")(8)
  val v2: Any = C[Double]("b")(4.5)

  def main(args: Array[String]): Unit = {
    tt.cast(v1).foreach { c: C[Int] =>
      val i: Int = c.y
      println(i)
    }
    tt.cast(v2).foreach { c: C[Int] =>
      val i: Int = c.y
      println(i)
    }
  }
}
