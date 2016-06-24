package com.example.shless

import shapeless._

class NestedGeneric {

  def foo(): Unit = {

    sealed trait U
    object U {
      final case class C(i: Int) extends U
      final case object D extends U
    }

    // if uncommented, compilation fails:
    // Generic[U]
  }
}
