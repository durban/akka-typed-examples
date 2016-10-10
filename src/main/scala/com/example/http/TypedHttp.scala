package com.example.http

import akka.typed._
import akka.typed.ScalaDSL._
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.MemberUp
import scala.concurrent.Await
import akka.http.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.Http.ServerBinding
import com.example.actors._
import akka.stream.Materializer
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.RouteResult
import scala.util.Success
import scala.util.Failure

object TypedHttp {

  sealed trait Message
  final case class Binding(b: ServerBinding) extends Message
  final case class Request(s: String) extends Message
  case object Error extends Message

  lazy val guardian: Behavior[Message] = FullTotal {
    case Sig(ctx, PreStart) =>
      implicit val sys: akka.actor.ActorSystem = ctx.system.untyped
      implicit val m: Materializer = ActorMaterializer()(ctx.untyped)
      implicit val ec = ctx.executionContext
      val self = ctx.self
      val route = path("hello") {
        get {
          entity(as[String]) { s =>
            self ! Request(s)
            complete { s"Hello, $s!\n" }
          }
        }
      }
      Http().bindAndHandle(route, "localhost", 8888) andThen {
        case Success(b) =>
          self ! Binding(b)
        case Failure(ex) =>
          self ! Error
      }
      Same
    case Sig(_, _) =>
      Unhandled
    case Msg(_, Binding(ServerBinding(addr))) =>
      println(s"Bound to $addr")
      Same
    case Msg(_, Request(s)) =>
      println(s"Got request: $s")
      Same
    case Msg(_, Error) =>
      Stopped
  }

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem[Message]("MySystem", guardian)
    Thread.sleep(5000)
    sys ! Error
  }
}
