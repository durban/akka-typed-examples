package com.example.sc

import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.codecs.implicits._
import scala.reflect.ClassTag

object ScodecSerializer {

  abstract class ScSerializer[A <: AnyRef](codec: Codec[A])(implicit tag: ClassTag[A])
      extends akka.serialization.Serializer {

    override val includeManifest: Boolean = false

    override val identifier: Int = 0x5eebfae5

    override def toBinary(o: AnyRef): Array[Byte] = o match {
      case a: A =>
        codec.encode(a).fold(
          err => throw new IllegalArgumentException(err.message),
          res => res.toByteArray
        )
      case x =>
        val cls = if (x eq null) "null" else x.getClass.getName
        throw new IllegalArgumentException(
          s"${getClass.getName} expected a(n) ${tag.runtimeClass.getName}, got a(n) ${cls}"
        )
    }

    override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
      codec.decodeValue(BitVector(bytes)).fold(
        err => throw new IllegalArgumentException(err.message),
        res => res
      )
    }
  }
}
