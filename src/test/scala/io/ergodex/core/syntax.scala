package io.ergodex.core

import io.ergodex.core.sim.{BoxRuntime, BoxSim, RuntimeCtx, SigmaProp}
import scorex.crypto.hash.Blake2b256

import scala.util.Try

object syntax {

  implicit class ToValidatorOps[F[_]](box: BoxSim[F]) {

    def R4[T]: Option[T] = getR(4)

    def R5[T]: Option[T] = getR(5)

    def R6[T]: Option[T] = getR(6)

    def R7[T]: Option[T] = getR(7)

    def R8[T]: Option[T] = getR(8)

    def R9[T]: Option[T] = getR(9)

    private def getR[T](i: Int): Option[T] =
      box.registers.get(i).flatMap(a => Try(a.asInstanceOf[T]).toOption)
  }

  def OUTPUTS(i: Int)(implicit ctx: RuntimeCtx): BoxSim[BoxRuntime.NonRunnable] =
    ctx.outputs(i)

  def OUTPUTS(implicit ctx: RuntimeCtx): Coll[BoxSim[BoxRuntime.NonRunnable]] =
    ctx.outputs.toVector

  def INPUTS(i: Int)(implicit ctx: RuntimeCtx): BoxSim[BoxRuntime.NonRunnable] =
    ctx.inputs(i)

  def INPUTS(implicit ctx: RuntimeCtx): Coll[BoxSim[BoxRuntime.NonRunnable]] =
    ctx.inputs.toVector

  def HEIGHT(implicit ctx: RuntimeCtx): Int = ctx.height

  def sigmaProp(x: Boolean): Boolean = x

  def blake2b256(xs: Coll[Byte]): Coll[Byte] = Blake2b256.hash(xs.inner.toArray).toVector

  def getVar[T](i: Byte)(implicit ctx: RuntimeCtx): Option[T] =
    ctx.vars.get(i.toInt).flatMap(a => Try(a.asInstanceOf[T]).toOption)

  def max(x: Int, y: Int): Int = math.max(x, y)

  def max(x: Long, y: Long): Long = math.max(x, y)

  implicit class ToBigIntOps[N: Numeric](private val n: N) {
    def toBigInt: BigInt = BigInt(implicitly[Numeric[N]].toLong(n))
  }

  @inline def min(x: BigInt, y: BigInt): BigInt =
    if (y < x) y else x

  implicit def sigmaPropIsBoolean(prop: SigmaProp)(implicit ctx: RuntimeCtx): Boolean =
    ctx.signatories.contains(prop)

  implicit class ToSigmaPropOps(prop: SigmaProp) {
    def propBytes: Coll[Byte] = prop.value.getBytes().toVector
  }

  final case class CollOpaque[+A](inner: Vector[A]) {
    def fold[A1 >: A](z: A1, op: (A1, A1) => A1): A1 = inner.fold(z)(op)
    def map[B](f: A => B): CollOpaque[B]             = inner.map(f)
    def apply(i: Int): A                             = inner.apply(i)
    def size: Int                                    = inner.size
  }

  implicit def toColl[A](vec: Vector[A]): Coll[A] = CollOpaque(vec)

  type Coll[A] = CollOpaque[A]

  object Coll {
    def apply[A](elems: A*): Coll[A] = CollOpaque(Vector.apply(elems:_*))
  }
}
