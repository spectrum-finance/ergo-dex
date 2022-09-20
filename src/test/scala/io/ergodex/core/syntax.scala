package io.ergodex.core

import io.ergodex.core.sim.{Box, BoxRuntime, RuntimeCtx, SigmaProp}

import scala.util.Try

object syntax {

  implicit class ToValidatorOps[F[_]](box: Box[F]) {

    def R4[T]: Option[T] = getR(4)

    def R5[T]: Option[T] = getR(5)

    def R6[T]: Option[T] = getR(6)

    def R7[T]: Option[T] = getR(7)

    def R8[T]: Option[T] = getR(8)

    def R9[T]: Option[T] = getR(9)

    private def getR[T](i: Int): Option[T] =
      box.registers.get(i).flatMap(a => Try(a.asInstanceOf[T]).toOption)
  }

  def OUTPUTS(i: Int)(implicit ctx: RuntimeCtx): Box[BoxRuntime.NonRunnable] =
    ctx.outputs(i)

  def INPUTS(i: Int)(implicit ctx: RuntimeCtx): Box[BoxRuntime.NonRunnable] =
    ctx.inputs(i)

  def HEIGHT(implicit ctx: RuntimeCtx): Int = ctx.height

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

  type Coll[A] = Vector[A]
}
