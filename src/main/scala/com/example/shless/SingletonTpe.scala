package com.example.shless

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import shapeless.Typeable

object SingletonTpe extends App {

  val v = new AnyRef with Serializable
  val t1: Typeable[v.type] = Typeable[v.type]
  val t2: Typeable[v.type] = javaDeserialize(javaSerialize(t1))

  // this is OK:
  assert(t1.cast((v: Any)).isDefined)

  // this fails:
  assert(t2.cast((v: Any)).isDefined)

  def javaSerialize(x: AnyRef): Array[Byte] = {
    val b = new ByteArrayOutputStream
    val o = new ObjectOutputStream(b)
    o.writeObject(x)
    o.close()
    b.close()
    b.toByteArray()
  }

  def javaDeserialize[A](bs: Array[Byte]): A = {
    val b = new ByteArrayInputStream(bs)
    val i = new ObjectInputStream(b)
    val res = i.readObject()
    res.asInstanceOf[A]
  }
}
