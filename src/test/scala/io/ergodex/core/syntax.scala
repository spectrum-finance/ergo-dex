package io.ergodex.core

object syntax {

  implicit class ToBigIntOps[N: Numeric](private val n: N) {
    def toBigInt: BigInt = BigInt(implicitly[Numeric[N]].toLong(n))
  }

  @inline def min(x: BigInt, y: BigInt): BigInt =
    if (y < x) y else x
}
