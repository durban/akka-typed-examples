package com.example.actors

import akka.typed._
import akka.typed.ScalaDSL._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import scala.concurrent.Await
import scala.concurrent.duration._

object TypedCluster {

  lazy val guardian: Behavior[MemberUp] = FullTotal {
    case Sig(ctx, PreStart) =>
      val c = Cluster(ctx.system.untyped)
      c.subscribe(ctx.self.untyped, classOf[MemberUp])
      Same
    case Sig(_, _) =>
      Unhandled
    case Msg(_, MemberUp(m)) =>
      Same
  }

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem("MySystem", guardian)
    Thread.sleep(2000)
    Await.ready(sys.terminate(), 1.second)
  }
}
