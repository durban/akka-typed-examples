package com.example.actors

import akka.actor._
import akka.cluster.ClusterEvent.MemberUp
import com.typesafe.config._
import scala.concurrent.Await
import scala.concurrent.duration._

object UntypedCluster {

  class MyActor extends Actor with ActorLogging {

    val cluster = akka.cluster.Cluster(context.system)

    override def preStart(): Unit =
      cluster.subscribe(self, classOf[MemberUp])

    override def postStop(): Unit =
      cluster.unsubscribe(self)

    def receive = {
      case MemberUp(m) =>
        log.info(s"Member is Up: ${m.address}")
    }
  }

  def main(args: Array[String]): Unit = {
    val sys = ActorSystem("MySystem", ConfigFactory.load())
    sys.actorOf(Props[MyActor], name = "MyActor")
    Thread.sleep(2000)
    Await.ready(sys.terminate(), 1.second)
  }
}
