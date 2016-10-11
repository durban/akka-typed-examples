package com.example.actors

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.typed._
import akka.typed.ScalaDSL._
import akka.typed.adapter._

object CtxAware2 {

  def b(ctx: ActorContext[Int], x: Int): Behavior[Int] = Total {
    case 0 =>
      println(s"Stopping (x = $x) ...")
      Stopped
    case n =>
      println(s"$n (x = $x)")
      b(ctx, x + 1)
  }

  def b(x: Int = 0): Behavior[Int] =
    ContextAware(ctx => b(ctx, x))

  def main(args: Array[String]): Unit = {
    val sys = akka.actor.ActorSystem("boo")
    val ref = sys.spawn(b(), "b")

    ref ! 1
    ref ! 2
    ref ! 0

    Thread.sleep(5000)
    Await.result(sys.terminate(), Duration.Inf)
  }
}
