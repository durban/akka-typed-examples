package com.example.shless

import shapeless._

object TypeableExample5 {

  trait T {
    type B
    class C(val b: B)
  }

  class X[A] extends T {
    final override type B = A
  }

  val tt = Typeable[X[Int]#C]

  val x1 = new X[Int]
  val v1: Any = new x1.C(8)
  val x2 = new X[Double]
  val v2: Any = new x2.C(4.5)

  def main(args: Array[String]): Unit = {
    tt.cast(v1).foreach { c: X[Int]#C =>
      val i: Int = c.b
      println(i)
    }
    tt.cast(v2).foreach { c: X[Int]#C =>
      val i: Int = c.b
      println(i)
    }
  }
}
