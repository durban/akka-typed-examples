package com.example.fs2ex

import scala.language.higherKinds

import fs2._
import fs2.util.Attempt
import fs2.util.Async

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

  type Pipe2[F[_],-I,-I2,+O] = (Stream[F,I], Stream[F,I2]) => Stream[F,O]

  /** Like `merge`, but halts as soon as _either_ branch halts. */
  def mergeHaltBoth[F[_]: Async, O]: Pipe2[F, O, O, O] = { (s1, s2) =>

    def go(l: Handle.AsyncStep1[F, O], r: Handle.AsyncStep1[F, O]): Pull[F, O, Nothing] = {
      for {
        it <- (l race r).pull
        x <- it match {
          case Left(l) => l.flatMap {
            case (Some(l), h) => Pull.output1(l) >> h.await1Async.flatMap(go(_, r))
            case (None, h) => Pull.done
          }
          case Right(r) => r.flatMap {
            case (Some(r), h) => Pull.output1(r) >> h.await1Async.flatMap(go(_, l))
            case (None, h) => Pull.done
          }
        }
      } yield x
    }

    s1.pull2(s2) { (h1, h2) =>
      for {
        l <- h1.await1Async
        r <- h2.await1Async
        x <- go(l, r)
      } yield {
        x
      }
    }
  }

  /**
   * Let through the `s2` branch as long as the `s1` branch is `false`,
   * listening asynchronously for the left branch to become `true`.
   * This halts as soon as either branch halts.
   */
  def interrupt[F[_]: Async, I]: Pipe2[F, Boolean, I, I] = { (s1, s2) =>
    mergeHaltBoth[F, Either[Boolean, I]].apply(s1.takeWhile(identity).map(Left.apply), s2.map(Right.apply)).flatMap {
      case Left(_) => Stream.empty
      case Right(e) => Stream(e)
    }
  }
}

object Test extends App {

  implicit val S = fs2.Strategy.fromFixedDaemonPool(8, threadName = "fs2worker")

  def rnd() = (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 10).toLong

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

  val r8: Vector[Int] = Exercise.mergeHaltBoth[Task, Int].apply(
    Stream.eval(Task.delay { Thread.sleep(10); 0 }) ++ Stream.pure(1, 2, 3),
    Stream.eval(Task.delay { Thread.sleep(100); 9 })
  ).runLog.unsafeRun()
  assert(r8 == Vector(0, 1, 2, 3))
  println(r8)

  val r9: Vector[Int] = Exercise.interrupt[Task, Int].apply(
    Stream.unfoldEval(0)(i => Task.delay {
      if (i < 5) {
        Thread.sleep(rnd())
        Some((true, i + 1))
      } else {
        Thread.sleep(rnd())
        None
      }
    }),
    Stream.repeatEval(Task.delay { Thread.sleep(rnd()); 5 })
  ).runLog.unsafeRun()
  assert(r9.forall { _ == 5 })
  println(r9)
}
