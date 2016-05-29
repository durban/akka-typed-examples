package com.example.shless

import shapeless._

object TypeableExample3 {

  final case class C[A](s: String) {
    var y: A = _
  }

  // Fixed in shapeless 2.3.1:

//  val tt = Typeable[C[Int]]
//
//  val v1: Any = { val r = C[Int]("a"); r.y = 8; r }
//  val v2: Any = { val r = C[Double]("b"); r.y = 4.5; r }
//
//  def main(args: Array[String]): Unit = {
//    tt.cast(v1).foreach { c: C[Int] =>
//      val i: Int = c.y
//      println(i)
//    }
//    tt.cast(v2).foreach { c: C[Int] =>
//      val i: Int = c.y
//      println(i)
//    }
//  }
}
