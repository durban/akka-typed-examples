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
import com.example.sc.ScodecSerializer
import akka.serialization.JavaSerializer
import scala.util.Try
import scala.util.Success
import scala.util.Failure

object TypedRemote {

  object Protocol {

    import scodec._
    import scodec.bits._
    import scodec.codecs._
    import scodec.codecs.implicits._

    sealed trait Message extends Product with Serializable
    case object Start extends Message
    final case class Boo(replyTo: ActorRef[Message]) extends Message

    implicit private[this] def arefCodec[A](implicit sys: akka.actor.ExtendedActorSystem): Codec[ActorRef[A]] = {
      // TODO: this is ugly
      val js = new JavaSerializer(sys)
      val lengthCodec = int64L
      Codec(
        (ref: ActorRef[A]) => {
          val u: akka.actor.ActorRef = ref.untyped
          val bytes = ByteVector(js.toBinary(u))
          for {
            size <- lengthCodec.encode(bytes.length)
          } yield size ++ bytes.toBitVector
        },
        (bits: BitVector) => {
          for {
            res <- lengthCodec.decode(bits)
            length = res.value
            (refBits, rest) = res.remainder.splitAt(length * 8)
            ref <- Try(js.fromBinary(refBits.toByteArray, None)) match {
              case Success(r) => r match {
                case untypedRef: akka.actor.ActorRef =>
                  Attempt.successful(ActorRef[A](untypedRef))
                case x =>
                  Attempt.failure(Err(s"Expected an ActorRef, got a(n) ${x.getClass.getName}"))
              }
              case Failure(ex) =>
                Attempt.failure(Err(ex.getMessage))
            }
          } yield DecodeResult(ref, rest)
        }
      )
    }

    private[this] def msgCodec(implicit sys: akka.actor.ExtendedActorSystem): Codec[Message] =
      Codec.coproduct[Message].discriminatedByIndex(uint8)

    final class MessageSerializer(sys: akka.actor.ExtendedActorSystem)
      extends ScodecSerializer.ScSerializer[Message](msgCodec(sys))
  }

  import Protocol._

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
