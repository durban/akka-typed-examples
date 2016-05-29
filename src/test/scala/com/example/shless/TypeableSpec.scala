package com.example.shless

import org.scalatest.Matchers
import org.scalatest.WordSpec
import shapeless._

class TypeableSpec extends WordSpec with Matchers {

  import TypeableSpec._

  "shapeless.Typeable" when {

    "generic case class" should {

      "multiple param lists" in {
        final case class C[A](i: Int)(val a: A)
        """Typeable[C[Int]]""" shouldNot typeCheck
        checkCast[C[Int]](C[Double](5)(5.5)) { c: C[Int] => val i: Int = c.a }
      }

      "context bounds" in {
        final case class C[A: Default](i: Int) {
          def y: A = implicitly[Default[A]].default
        }
        checkCast[C[Int]](C[Double](5)(Default.DefaultDouble)) { c: C[Int] =>
          val i: Int = c.y
        }
      }

      "vars" in {
        final case class C[A](i: Int) {
          var y: A = _
        }
        checkCast[C[Int]]({ val c = C[Double](1); c.y = 0.5; c }) { c: C[Int] =>
          val i: Int = c.y
        }
      }

      "generic" in {
        // NB: this is already correct
        final case class C[A](i: Int, as: List[A])
        checkCast[C[Int]](C[Double](1, List(0.5))) { c: C[Int] =>
          val i: Int = c.as.head
        }
      }

      "simple def" in {
        // this works now, and should remain so
        final case class C[A](i: Int, a: A) {
          def ff: A = a
        }
        assert(Typeable[C[Int]].cast(C[Double](5, 5.5): Any).isEmpty)
      }

      "inherited" in {
        abstract class S[A](a: A) {
          val x: A = a
        }
        final case class C[A](i: Int)(a: A) extends S[A](a)
        checkCast[C[Int]](C[Double](5)(0.5)) { c: C[Int] =>
          val i: Int = c.x
        }
      }
    }

    "abstract type" should {

      "nested case class" in {
        trait T {
          type B
          case class C(i: Int, b: B)
        }
        final class X[A] extends T {
          final override type B = A
        }
        checkCast[X[Int]#C](new X[Double].C(1, 1.5)) { c: X[Int]#C =>
          val i: Int = c.b
        }
      }

      "nested normal class" in {
        trait T {
          type B
          class C(val b: B)
        }
        final class D[A] extends T {
          final override type B = A
        }
        checkCast[D[Int]#C]({val d = new D[Double]; new d.C(4.5)}) { c: D[Int]#C =>
          val i: Int = c.b
        }
      }
    }

    "normal" should {

      // these should work fine (???)

      "unrelated def" in {
        final case class C[A](i: Int) {
          def x: Int = 89
        }
        checkCast[C[Int]](C[Double](8)) { c: C[Int] =>
          val i: Int = c.x
        }

        final case class C2[A](a: A) {
          def x: Int = 89
        }
        assert(Typeable[C2[Int]].cast(C2[Double](4.5): Any).isEmpty)
      }

      "unrelated val" in {
        final case class C[A](a: A)(val i: Int)
        assert(Typeable[C[Int]].cast(C[Double](4.5)(5): Any).isEmpty)

        final case class C2[A](a: A) {
          val i: Int = 89
        }
        assert(Typeable[C2[Int]].cast(C2[Double](4.5): Any).isEmpty)
      }
    }
  }
}

object TypeableSpec {

  def checkCast[A: Typeable](x: Any)(f: A => Unit): Unit = {
    Typeable[A].cast(x).foreach { a: A =>
      f(a)
    }
  }

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
}
