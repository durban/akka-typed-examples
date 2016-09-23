package com.example.fs2ex

import scala.language.higherKinds

import fs2._
import fs2.util.Attempt

object Exercise {

  def repeat[F[_], O](s: Stream[F, O]): Stream[F, O] =
    s ++ repeat(s)

  def drain[F[_], O](s: Stream[F, O]): Stream[F, Nothing] =
    s.flatMap { _ => Stream.empty[F, Nothing] }

  def eval_[F[_], O](eff: F[O]): Stream[F, Nothing] =
    drain(Stream.eval[F, O](eff))

  def attempt[F[_], O](s: Stream[F, O]): Stream[F, Attempt[O]] =
    s.map(Attempt.success).onError(ex => Stream(Attempt.failure(ex)))

  ////

  def takeWhile[F[_], O](s: Stream[F, O])(p: O => Boolean): Stream[F, O] =
    s.pull(takeWhile(p))

  def takeWhile[F[_], O](p: O => Boolean)(h: Handle[F, O]): Pull[F, O, Nothing] = {
    for {
      (o, h) <- h.await1
      _ <- if (p(o)) Pull.output1(o) else Pull.done
      x <- takeWhile(p)(h)
    } yield x
  }

  def intersperse[F[_], O](s: Stream[F, O])(o: O): Stream[F, O] =
    s.pull(intersperse(o)(true))

  def intersperse[F[_], O](item: O)(first: Boolean)(h: Handle[F, O]): Pull[F, O, Nothing] = {
    for {
      (chunk, h) <- h.await
      z <- {
        val x = Chunk.seq(chunk.foldLeft[List[O]](Nil){ (acc, o) =>
          o :: item :: acc
        }.reverse)
        val y = if (first) x.drop(1) else x
        Pull.output(y) >> intersperse(item)(false)(h)
      }
    } yield z
  }

  def scan[F[_], O, X](s: Stream[F, O])(z: X)(f: (X, O) => X): Stream[F, X] =
    s.pull(scan(z)(f))

  def scan[F[_], O, X](state: X)(f: (X, O) => X)(h: Handle[F, O]): Pull[F, X, Nothing] = {
    for {
      opt <- h.await1Option
      d <- opt match {
        case Some((o, h)) =>
          Pull.output1(state) >> scan(f(state, o))(f)(h)
        case None =>
          // end of stream
          Pull.output1(state) >> Pull.done
      }
    } yield d
  }

  ////
}

object Test extends App {

  val r1: List[Int] = Exercise.repeat(Stream(1,0)).take(6).toList
  assert(r1 == List(1, 0, 1, 0, 1, 0))
  println(r1)

  val r2: List[Nothing] = Exercise.drain(Stream(1,2,3)).toList
  assert(r2 == List())
  println(r2)

  val r3: Vector[Nothing] = Exercise.eval_(Task.delay(println("!!"))).runLog.unsafeRun()
  assert(r3 == Vector())
  println(r3)

  val exc = new Exception("nooo!!!")
  val r4: List[Attempt[Int]] = Exercise.attempt((Stream(1,2) ++ (throw exc))).toList
  assert(r4 == List(Right(1), Right(2), Left(exc)))
  println(r4)

  ////
  println("============")
  ////

  val r5: List[Int] = Exercise.takeWhile(Stream.range(0,100))(_ < 7).toList
  assert(r5 == List(0, 1, 2, 3, 4, 5, 6))
  println(r5)

  val r6: List[String] = Exercise.intersperse(Stream("Alice","Bob","Carol"))("|").toList
  assert(r6 == List("Alice", "|", "Bob", "|", "Carol"))
  println(r6)

  val r7: List[Int] = Exercise.scan(Stream.range(1,10))(0)(_ + _).toList
  assert(r7 == List(0, 1, 3, 6, 10, 15, 21, 28, 36, 45), r7.toString)
  println(r7)

  ////
  println("============")
  ////
}
