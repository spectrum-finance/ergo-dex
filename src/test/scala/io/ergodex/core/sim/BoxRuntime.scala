package io.ergodex.core.sim

import io.ergodex.core.syntax.Coll

object BoxRuntime {
  type NonRunnable[_] = Any
}

final case class TaggedValidator[F[_]](tag: String, validator: F[Boolean])

trait Box[+F[_]] { self =>
  val id: Coll[Byte]
  val value: Long
  val creationHeight: Int
  val tokens: Vector[(Coll[Byte], Long)]
  val registers: Map[Int, Any]
  val validatorTag: String
  val validator: F[Boolean]

  final def SELF: Box[F] = self

  final def propositionBytes: Coll[Byte] = validatorTag.getBytes().toVector

  final def setRegister[T](reg: Int, v: T): Box[F] =
    new Box[F] {
      override val id: Coll[Byte]                     = self.id
      override val value: Long                        = self.value
      override val creationHeight: Int                = self.creationHeight
      override val tokens: Vector[(Coll[Byte], Long)] = self.tokens
      override val registers: Map[Int, Any]           = self.registers + (reg -> v)
      override val validatorTag: String               = self.validatorTag
      override val validator: F[Boolean]              = self.validator
    }
}

object Box {}

trait ToLedger[A, F[_]] {
  def toLedger(a: A): Box[F]
}

object ToLedger {
  implicit def apply[A, F[_]](implicit ev: ToLedger[A, F]): ToLedger[A, F] = ev

  implicit class ToLedgerOps[A](a: A) {
    def toLedger[F[_]](implicit ev: ToLedger[A, F]): Box[F] = ev.toLedger(a)
  }
}
