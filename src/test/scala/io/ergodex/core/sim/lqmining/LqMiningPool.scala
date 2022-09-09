package io.ergodex.core.sim.lqmining

import cats.kernel.Monoid
import io.ergodex.core.sim.lqmining.LMPool.MaxCapTT
import io.ergodex.core.sim.{RuntimeCtx, RuntimeState, ToLedger, TokenId}

final case class LMConfig(
  frameLen: Int,
  epochLen: Int,
  epochNum: Int,
  programStart: Int,
  programBudget: Long
) {
  val programEnd: Int  = programStart + frameLen * epochNum * epochLen
  val framesNum: Int   = epochNum * epochLen
  val epochAlloc: Long = programBudget / epochNum
}

final case class PoolReserves(X: Long, LQ: Long, vLQ: Long, TT: Long) {
  val emissionTT: Long = MaxCapTT - TT
}

final case class StakingBundle(vLQ: Long, TT: Long)

object StakingBundle {
  implicit val monoid: Monoid[StakingBundle] =
    new Monoid[StakingBundle] {
      def empty: StakingBundle                                       = StakingBundle(0L, 0L)
      def combine(x: StakingBundle, y: StakingBundle): StakingBundle = StakingBundle(x.vLQ + y.vLQ, x.TT + y.TT)
    }
}

object Token {
  type X
  type LQ
  type TT
}

final case class AssetInput[T](value: Long)
final case class AssetOutput[T](value: Long)
object AssetOutput {
  implicit def monoid[T]: Monoid[AssetOutput[T]] =
    new Monoid[AssetOutput[T]] {
      def empty: AssetOutput[T]                                         = AssetOutput(0L)
      def combine(x: AssetOutput[T], y: AssetOutput[T]): AssetOutput[T] = AssetOutput(x.value + y.value)
    }
}
final case class BurnAsset[T](value: Long)

final case class LMPool[Ledger[_]: RuntimeState](
  conf: LMConfig,
  reserves: PoolReserves,
  lqAllocSum: Long,
  lastUpdatedAtFrameIx: Int,
  lastUpdatedAtEpochIx: Int
) {
  import LMPool._

  def updateReserves(fn: PoolReserves => PoolReserves): LMPool[Ledger] =
    copy(reserves = fn(reserves))

  private def frameAndEpochIx(ctx: RuntimeCtx): (Int, Int) = {
    val curFrameIxNum = ctx.height - conf.programStart + 1
    val curFrameIxRem = curFrameIxNum % conf.frameLen
    val curFrameIxR   = curFrameIxNum / conf.frameLen
    val curFrameIx    = if (curFrameIxRem > 0) curFrameIxR + 1 else curFrameIxR
    val curEpochIxRem = curFrameIx    % conf.epochLen
    val curEpochIxR   = curFrameIx / conf.epochLen
    val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR
    curFrameIx -> curEpochIx
  }

  def deposit(lq: AssetInput[Token.LQ]): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    RuntimeState.withRuntimeState { ctx =>
      if (ctx.height < conf.programEnd) {
        val (curFrameIx, curEpochIx) = frameAndEpochIx(ctx)
        val epochsToCompound         = conf.epochNum - curEpochIx
        if (reserves.X - epochsToCompound * conf.epochAlloc <= conf.epochAlloc) {
          val releasedVLQ     = lq.value
          val framesAllocated = conf.framesNum - math.max(0, curFrameIx)
          val releasedTT      = releasedVLQ * framesAllocated
          val (lqAllocSum_, frameIx_, epochIx_) =
            if (curEpochIx != lastUpdatedAtEpochIx) {
              val passedFrames = curFrameIx - (curEpochIx - 1) * conf.epochLen
              (passedFrames * reserves.LQ, curFrameIx, curFrameIx)
            } else if (curFrameIx != lastUpdatedAtFrameIx) {
              val passedFrames = curFrameIx - lastUpdatedAtFrameIx
              (lqAllocSum + passedFrames * reserves.LQ, curFrameIx, lastUpdatedAtEpochIx)
            } else {
              (lqAllocSum, lastUpdatedAtFrameIx, lastUpdatedAtEpochIx)
            }
          Right(
            copy(
              reserves = reserves.copy(
                LQ  = reserves.LQ + lq.value,
                vLQ = reserves.vLQ - releasedVLQ,
                TT  = reserves.TT - releasedTT
              ),
              lqAllocSum           = lqAllocSum_,
              lastUpdatedAtFrameIx = frameIx_,
              lastUpdatedAtEpochIx = epochIx_
            ) ->
            StakingBundle(releasedVLQ, releasedTT)
          )
        } else {
          Left(PrevEpochNotWithdrawn)
        }
      } else Left(ProgramEnded)
    }

  def compound(
    bundle: StakingBundle,
    epoch: Int
  ): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle, AssetOutput[Token.X])]] =
    RuntimeState.withRuntimeState { ctx =>
      val (_, curEpochIx)  = frameAndEpochIx(ctx)
      val epochsToCompound = conf.epochNum - epoch
      if (epoch <= curEpochIx - 1) {
        if (reserves.X - epochsToCompound * conf.epochAlloc <= conf.epochAlloc) {
          val framesBurned = (bundle.TT / bundle.vLQ) - conf.epochLen * epochsToCompound
          val revokedTT    = framesBurned * bundle.vLQ
          val lqAllocSum_ =
            if (lastUpdatedAtEpochIx != (curEpochIx - 1)) {
              reserves.LQ * conf.epochLen // reserves haven't been updated for the whole past epoch.
            } else if (lastUpdatedAtFrameIx != epoch * conf.epochLen) {
              val framesUntouched = epoch * conf.epochLen - lastUpdatedAtFrameIx
              reserves.LQ * framesUntouched + lqAllocSum
            } else {
              lqAllocSum
            }
          val reward = (BigInt(conf.epochAlloc) * revokedTT / lqAllocSum_).toLong
          Right(
            (
              updateReserves(r => PoolReserves(X = r.X - reward, r.LQ, r.vLQ, TT = r.TT + revokedTT)),
              bundle.copy(TT = bundle.TT - revokedTT),
              AssetOutput(reward)
            )
          )
        } else {
          Left(PrevEpochNotWithdrawn)
        }
      } else {
        Left(IllegalEpoch)
      }
    }

  def redeem(bundle: StakingBundle): Ledger[VerifiedST[(LMPool[Ledger], AssetOutput[Token.LQ])]] =
    RuntimeState.withRuntimeState { ctx =>
      val (curFrameIx, _)   = frameAndEpochIx(ctx)
      val releasedLQ        = bundle.vLQ
      val framesDeallocated = conf.epochNum * conf.epochLen - math.max(0, curFrameIx)
      val lqAllocSum_ =
        if (lastUpdatedAtFrameIx == curFrameIx) lqAllocSum - releasedLQ
        else lqAllocSum
      Right(
        (
          copy(
            reserves = reserves.copy(
              LQ  = reserves.LQ - bundle.vLQ,
              vLQ = reserves.vLQ + bundle.vLQ,
              TT  = reserves.TT + bundle.TT
            ),
            lqAllocSum = lqAllocSum_
          ),
          AssetOutput(releasedLQ)
        )
      )
    }
}

object LMPool {
  val MaxCapVLQ: Long = Long.MaxValue
  val MaxCapTT: Long  = Long.MaxValue

  sealed trait LMPoolErr
  case object ProgramEnded extends LMPoolErr
  case object PrevEpochNotWithdrawn extends LMPoolErr
  case object EpochAlreadyWithdrawn extends LMPoolErr
  case object IllegalEpoch extends LMPoolErr

  type VerifiedST[+A] = Either[LMPoolErr, A]

  val MinCollateralErg = 5000000L

  implicit def toLedger[F[_]: RuntimeState]: ToLedger[LMPool[F], F] =
    (pool: LMPool[F]) =>
      new LqMiningPoolBox[F](
        MinCollateralErg,
        tokens = Vector(
          TokenId("nft") -> 1L,
          TokenId("x")   -> pool.reserves.X,
          TokenId("lq")  -> pool.reserves.LQ,
          TokenId("vql") -> pool.reserves.vLQ,
          TokenId("tt")  -> pool.reserves.TT
        ),
        registers = Map(
          4 -> Vector(
            pool.conf.frameLen,
            pool.conf.epochLen,
            pool.conf.epochNum,
            pool.conf.programStart,
            pool.conf.programBudget
          ),
          5 -> BigInt(pool.lqAllocSum),
          6 -> pool.lastUpdatedAtFrameIx,
          7 -> pool.lastUpdatedAtEpochIx
        )
      )

  def init[Ledger[_]: RuntimeState](
    frameLen: Int,
    epochLen: Int,
    epochNum: Int,
    programStart: Int,
    programBudget: Long
  ): LMPool[Ledger] =
    LMPool(
      LMConfig(frameLen, epochLen, epochNum, programStart, programBudget),
      PoolReserves(programBudget, 0L, MaxCapVLQ, MaxCapTT),
      lqAllocSum           = 0L,
      lastUpdatedAtFrameIx = 0,
      lastUpdatedAtEpochIx = 0
    )
}
