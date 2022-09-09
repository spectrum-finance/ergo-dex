package io.ergodex.core.sim

object BoxRuntime {
  type NonRunnable[_] = Any
}

final case class TokenId(value: String)

final case class TaggedValidator[F[_]](tag: String, validator: F[Boolean])

trait Box[+F[_]] { self =>
  val value: Long
  val tokens: Vector[(TokenId, Long)]
  val registers: Map[Int, Any]
  val validatorTag: String
  val validator: F[Boolean]

  type Coll[A] = Vector[A]

  def SELF: Box[F] = self

  def propositionBytes: String = validatorTag
}

trait ToLedger[A, F[_]] {
  def toLedger(a: A): Box[F]
}

object ToLedger {
  implicit def apply[A, F[_]](implicit ev: ToLedger[A, F]): ToLedger[A, F] = ev

  implicit class ToLedgerOps[A](a: A) {
    def toLedger[F[_]](implicit ev: ToLedger[A, F]): Box[F] = ev.toLedger(a)
  }
}
