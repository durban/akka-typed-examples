package com.example.http

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl._
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server._
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.util.{ Success, Failure, Try }

object Unbinding {

  implicit val system: ActorSystem = ActorSystem("sys", config = Some(ConfigFactory.empty))
  implicit val mat: Materializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    import system.dispatcher
    val handler: Route = complete("hello")
    val future: Future[Http.ServerBinding] = Http().bindAndHandle(handler, "localhost", 8888)
    val b = Await.result(future, 2.seconds)
    Thread.sleep(2000)
    println("Stopping actor system ...")
    Await.result(system.terminate(), 2.seconds)
    println("Unbinding ...")
    b.unbind()
  }
}
