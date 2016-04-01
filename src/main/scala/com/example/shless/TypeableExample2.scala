package com.example.shless

import shapeless._

object TypeableExample2 {

  trait Default[A] {
    def default: A
  }

  object Default {

    def apply[A](implicit ev: Default[A]) = ev

    implicit object DefaultInt extends Default[Int] {
      override def default: Int = 0
    }

    implicit object DefaultDouble extends Default[Double] {
      override def default: Double = 0.0
    }
  }

  final case class C[A: Default](s: String) {
    def y: A = implicitly[Default[A]].default
  }

  val tt = Typeable[C[Int]]

  val v1: Any = C[Int]("a")
  val v2: Any = C[Double]("b")

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
