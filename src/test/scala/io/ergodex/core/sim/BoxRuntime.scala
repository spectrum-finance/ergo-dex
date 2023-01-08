package io.ergodex.core.sim

import io.ergodex.core.sim.BoxRuntime.NonRunnable
import io.ergodex.core.syntax.Coll
import org.ergoplatform.ErgoBox
import scorex.util.encode.Base16
import sigmastate.Values.ConstantNode

import scala.util.Try

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
  val validatorBytes: String
  val validator: F[Boolean]
  val constants: Map[Int, Any] = Map.empty

  final def SELF: Box[F] = self

  final def creationInfo: (Int, Int) = (creationHeight, creationHeight)

  final def propositionBytes: Coll[Byte] = validatorBytes.getBytes().toVector

  final def getConstant[T](i: Int): Option[T] = constants.get(i).flatMap(c => Try(c.asInstanceOf[T]).toOption)

  final def setRegister[T](reg: Int, v: T): Box[F] =
    new Box[F] {
      override val id: Coll[Byte]                     = self.id
      override val value: Long                        = self.value
      override val creationHeight: Int                = self.creationHeight
      override val tokens: Vector[(Coll[Byte], Long)] = self.tokens
      override val registers: Map[Int, Any]           = self.registers + (reg -> v)
      override val validatorBytes: String             = self.validatorBytes
      override val validator: F[Boolean]              = self.validator
      override val constants: Map[Int, Any]           = self.constants
    }
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

trait TryFromBox[Box[_[_]], F[_]] { self =>
  def tryFromBox(bx: ErgoBox): Option[Box[F]]
  final def translate[ToBox[_[_]], G[_]](fk: Box[F] => ToBox[G]): TryFromBox[ToBox, G] =
    (bx: ErgoBox) => self.tryFromBox(bx).map(fk)
}

object TryFromBox {
  implicit class TryFromBoxOps[Box[+_[_]], F[_]](a: Box[F]) {
    def tryFromBox(bx: ErgoBox)(implicit ev: TryFromBox[Box, F]): Option[Box[F]] = ev.tryFromBox(bx)
  }
}

// Non-runnable projection of a box.
final case class AnyBox(
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Vector[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val validatorBytes: String,
  override val constants: Map[Int, Any]
) extends Box[BoxRuntime.NonRunnable] {
  override val validator: NonRunnable[Boolean] = ()
}

object AnyBox {
  implicit val tryFromBox: TryFromBox[Box, BoxRuntime.NonRunnable] =
    (bx: ErgoBox) =>
      Some(
        AnyBox(
          id             = bx.id.toVector,
          value          = bx.value,
          creationHeight = bx.creationHeight,
          validatorBytes = Base16.encode(bx.ergoTree.bytes),
          tokens         = bx.additionalTokens.toArray.map { case (id, v) => id.toVector -> v }.toVector,
          registers = bx.additionalRegisters.toVector.map { case (r, v) =>
            r.number.toInt -> {
              v match {
                case ConstantNode(array: special.collection.CollOverArray[Any @unchecked], _) => array.toArray.toVector
                case ConstantNode(v, _)                                                       => v
                case v                                                                        => v
              }
            }
          }.toMap,
          constants = bx.ergoTree.constants.toVector.zipWithIndex.map { case (c, ix) =>
            ix -> {
              c match {
                case ConstantNode(array: special.collection.CollOverArray[Any @unchecked], _) => array.toArray.toVector
                case ConstantNode(v, _)                                                       => v
                case v                                                                        => v
              }
            }
          }.toMap
        )
      )
}
