package com.example.shless

import shapeless._

object TypeableExample4 {

  trait T {
    type B
    case class C(b: B)
  }

  class X[A] extends T {
    final override type B = A
  }

  val tt = Typeable[X[Int]#C]

  val v1: Any = new X[Int].C(8)
  val v2: Any = new X[Double].C(4.5)

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
