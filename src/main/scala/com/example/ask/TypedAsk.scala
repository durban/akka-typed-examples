package com.example.ask


import akka.typed._
import akka.typed.AskPattern._
import akka.typed.ScalaDSL._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.actor.RootActorPath
import akka.actor.ActorPath
import akka.actor.Address
import akka.util.Timeout
import scala.util.Success
import com.typesafe.config.ConfigFactory

object TypedAsk {

  implicit val timeout: Timeout = Timeout(2.seconds)

  final case class Msg(s: String, replyTo: ActorRef[String])

  lazy val root: Behavior[String] = ContextAware { ctx => Static { s =>
    val noSuchActor = ActorRef[Msg](
      ctx.system.provider.resolveActorRef(RootActorPath(Address("akka.tcp", "MySys")))
    )
    val f = noSuchActor.?[String](Msg(s, _))
    f.andThen {
      case Success(s) =>
        println(s)
    }(ctx.executionContext)
  }}


  def main(args: Array[String]): Unit = {
    val sys = ActorSystem.apply("MySys", Props(root), config = Some(ConfigFactory.empty))
    sys ! "foo"
    Thread.sleep(2000)
    Await.ready(sys.terminate(), 1.second)
  }
}
