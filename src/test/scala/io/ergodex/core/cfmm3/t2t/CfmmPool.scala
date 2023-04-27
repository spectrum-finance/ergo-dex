package io.ergodex.core.cfmm3.t2t

import cats.kernel.Monoid
import io.ergodex.core.Helpers.{boxId, hex, tokenId}
import io.ergodex.core.{RuntimeState, ToLedger}

object Token {
  type X
  type Y
  type LP
}

final case class AssetInput[T](value: Long)

final case class AssetOutput[T](value: Long)

object AssetOutput {
  implicit def monoid[T]: Monoid[AssetOutput[T]] =
    new Monoid[AssetOutput[T]] {
      def empty: AssetOutput[T] = AssetOutput(0L)

      def combine(x: AssetOutput[T], y: AssetOutput[T]): AssetOutput[T] = AssetOutput(x.value + y.value)
    }
}

final case class PoolReserves(x: Long, y: Long, lp: Long)

final case class PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, burnLP: Long, minInitialDeposit: Long)

final case class CfmmPool[Ledger[_]: RuntimeState](reserves: PoolReserves, config: PoolConfig) {

  def updateReserves(fn: PoolReserves => PoolReserves): CfmmPool[Ledger] =
    copy(reserves = fn(reserves))

  val supplyLP: Long = config.emissionLP - reserves.lp

  import CfmmPool._

  def deposit(
    inX: AssetInput[Token.X],
    inY: AssetInput[Token.Y]
  ): Ledger[VerifiedST[(CfmmPool[Ledger], AssetOutput[Token.LP], AssetOutput[Some.type])]] =
    RuntimeState.withRuntimeState { _ =>
      val minByX   = inX.value * supplyLP / reserves.x
      val minByY   = inY.value * supplyLP / reserves.y
      val unlocked = math.min(minByX, minByY)
      val changeX =
        if (minByX < minByY) {
          val diff    = minByY - minByX
          val excessY = diff * reserves.y / supplyLP
          excessY
        } else 0L
      val changeY = if (minByX > minByY) {
        val diff    = minByX - minByY
        val excessX = diff * reserves.x / supplyLP
        excessX
      } else 0L

      Right(
        (
          copy(reserves =
            reserves.copy(reserves.x + inX.value - changeY, reserves.y + inY.value - changeX, reserves.lp - unlocked)
          ),
          AssetOutput(unlocked),
          AssetOutput(math.max(changeX, changeY))
        )
      )
    }

  def redeem(
    inLp: AssetInput[Token.LP]
  ): Ledger[VerifiedST[(CfmmPool[Ledger], AssetOutput[Token.X], AssetOutput[Token.Y])]] =
    RuntimeState.withRuntimeState { _ =>
      require(inLp.value <= supplyLP)
      val redeemedX = inLp.value * reserves.x / supplyLP
      val redeemedY = inLp.value * reserves.y / supplyLP
      Right(
        (
          copy(reserves = reserves.copy(reserves.x - redeemedX, reserves.y - redeemedY, reserves.lp + inLp.value)),
          AssetOutput(redeemedX),
          AssetOutput(redeemedY)
        )
      )
    }

  def swapX(inX: AssetInput[Token.X]): Ledger[VerifiedST[(CfmmPool[Ledger], AssetOutput[Token.Y])]] =
    RuntimeState.withRuntimeState { _ =>
      require(inX.value > 0)
      val (deltaX, deltaY) =
        (
          BigInt(inX.value),
          BigInt(reserves.y) * inX.value * config.feeNum /
          (BigInt(reserves.x) * config.feeDenom + inX.value * config.feeNum)
        )

      Right(
        (
          copy(reserves = reserves.copy(reserves.x + deltaX.toLong, reserves.y - deltaY.toLong)),
          AssetOutput(deltaY.toLong)
        )
      )
    }

  def swapY(inY: AssetInput[Token.Y]): Ledger[VerifiedST[(CfmmPool[Ledger], AssetOutput[Token.X])]] =
    RuntimeState.withRuntimeState { _ =>
      require(inY.value > 0)
      val (deltaX, deltaY) =
        (
          BigInt(reserves.x) * inY.value * config.feeNum /
          (BigInt(reserves.y) * config.feeDenom + inY.value * config.feeNum),
          BigInt(inY.value)
        )

      Right(
        (
          copy(reserves = reserves.copy(reserves.x - deltaX.toLong, reserves.y + deltaY.toLong)),
          AssetOutput(deltaX.toLong)
        )
      )
    }
}

object CfmmPool {

  val DefaultCreationHeight = 100

  sealed trait CfmmPoolErr

  type VerifiedST[+A] = Either[CfmmPoolErr, A]

  implicit def toLedger[F[_]: RuntimeState]: ToLedger[CfmmPool[F], F] =
    (pool: CfmmPool[F]) =>
      new CfmmPoolBox[F](
        boxId("cfmm_pool_box"),
        10L,
        DefaultCreationHeight,
        tokens = Vector(
          tokenId("pool_NFT") -> 1L,
          tokenId("lp")       -> pool.reserves.lp,
          tokenId("x")        -> pool.reserves.x,
          tokenId("y")        -> pool.reserves.y
        ),
        registers = Map(
          4 -> pool.config.feeNum
        ),
        constants      = Map.empty,
        validatorBytes = hex("cfmm_pool")
      )

  def init[Ledger[_]: RuntimeState](inX: Long, inY: Long, config: PoolConfig): CfmmPool[Ledger] = {
    require(inX >= config.minInitialDeposit && inY >= config.minInitialDeposit)

    val share = math.sqrt(inX * inY).toLong // todo: overflow
    CfmmPool(PoolReserves(inX, inY, config.emissionLP - config.burnLP - share), config)
  }
}
