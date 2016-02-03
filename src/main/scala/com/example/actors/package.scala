package com.example

import akka.typed._
import akka.actor.ExtendedActorSystem

package object actors {

  implicit class ActorRefOps(ref: ActorRef[_]) {
    def untyped: akka.actor.ActorRef =
      ref.getClass.getMethod("untyped").invoke(ref).asInstanceOf[akka.actor.ActorRef]
  }

  implicit class ActorSystemOps(sys: ActorSystem[_]) {
    def untyped: ExtendedActorSystem =
      sys.getClass.getMethod("untyped").invoke(sys).asInstanceOf[ExtendedActorSystem]
  }
}
