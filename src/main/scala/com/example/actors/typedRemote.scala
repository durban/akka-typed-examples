package com.example.actors

import akka.typed._
import akka.typed.ScalaDSL._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.Deploy
import akka.remote.RemoteScope
import akka.actor.Address
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import akka.actor.AddressFromURIString

object TypedRemote {

  sealed trait Message
  case object Start extends Message
  final case class Boo(replyTo: ActorRef[Message]) extends Message

  lazy val guardian: Behavior[Message] = ContextAware { ctx =>
    Total {
      case Start =>
        val ch = ctx.spawn(Props(child), "Child")
        ch ! Boo(ctx.self)
        Same
      case Boo(_) =>
        println("reponse boo")
        Same
    }
  }

  lazy val child: Behavior[Message] = SelfAware { self =>
    Static {
      case Start =>
        println("Start")
      case Boo(r) =>
        println("boo")
        r ! Boo(self)
    }
  }

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem(
      "MySystem",
      Props(guardian),
      config = Some(ConfigFactory.parseResources(getClass, "/local.conf")))
    Thread.sleep(1000)
    sys ! Start
    Thread.sleep(4000)
    Await.ready(sys.terminate(), 1.second)
  }
}

object RemoteSystem {

  lazy val dummy: Behavior[Nothing] = Static[Nothing] { _ => }

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem[Nothing](
      "MyRemote",
      Props[Nothing](dummy),
      config = Some(ConfigFactory.parseResources(getClass, "/remote.conf")))
    Thread.sleep(10000)
    Await.ready(sys.terminate(), 1.second)
  }
}
