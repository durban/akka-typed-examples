package com.example.actors

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.typed._
import akka.typed.ScalaDSL._
import akka.typed.adapter._

object CtxAware {

  def b(x: Int = 0): Behavior[Int] = ContextAware { ctx =>
    Total {
      case 0 =>
        println(s"Stopping (x = $x) ...")
        Stopped
      case n =>
        println(s"$n (x = $x)")
        b(x + 1)
    }
  }

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
