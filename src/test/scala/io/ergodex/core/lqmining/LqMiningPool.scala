package io.ergodex.core.lqmining

import cats.kernel.Monoid
import io.ergodex.core.Helpers.{boxId, tokenId}
import io.ergodex.core.lqmining.LMPool.MaxCapTMP
import io.ergodex.core.{RuntimeCtx, RuntimeState, ToLedger}
import io.ergodex.core.syntax.Coll

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

final case class PoolReserves(X: Long, LQ: Long, vLQ: Long, TMP: Long) {
  val emissionTMP: Long = MaxCapTMP - TMP
}

final case class StakingBundle(vLQ: Long, TMP: Long)

object StakingBundle {
  implicit val monoid: Monoid[StakingBundle] =
    new Monoid[StakingBundle] {
      def empty: StakingBundle                                       = StakingBundle(0L, 0L)
      def combine(x: StakingBundle, y: StakingBundle): StakingBundle = StakingBundle(x.vLQ + y.vLQ, x.TMP + y.TMP)
    }
}

object Token {
  type X
  type LQ
  type TMP
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
        val releasedVLQ              = lq.value
        val framesAllocated          = conf.framesNum - math.max(0, curFrameIx)
        val releasedTMP              = releasedVLQ * framesAllocated
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
              TMP = reserves.TMP - releasedTMP
            ),
            lqAllocSum           = lqAllocSum_,
            lastUpdatedAtFrameIx = frameIx_,
            lastUpdatedAtEpochIx = epochIx_
          ) ->
          StakingBundle(releasedVLQ, releasedTMP)
        )
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
          val framesBurned = (bundle.TMP / bundle.vLQ) - conf.epochLen * epochsToCompound
          val revokedTMP   = framesBurned * bundle.vLQ
          val lqAllocSum_ =
            if (lastUpdatedAtEpochIx != (curEpochIx - 1)) {
              reserves.LQ * conf.epochLen // reserves haven't been updated for the whole past epoch.
            } else if (lastUpdatedAtFrameIx != epoch * conf.epochLen) {
              val framesUntouched = epoch * conf.epochLen - lastUpdatedAtFrameIx
              reserves.LQ * framesUntouched + lqAllocSum
            } else {
              lqAllocSum
            }
          val reward = (BigInt(conf.epochAlloc) * revokedTMP / lqAllocSum_).toLong
          Right(
            (
              updateReserves(r => PoolReserves(X = r.X - reward, r.LQ, r.vLQ, TMP = r.TMP + revokedTMP)),
              bundle.copy(TMP = bundle.TMP - revokedTMP),
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
      val lqAllocSum_ =
        if (lastUpdatedAtFrameIx == curFrameIx) lqAllocSum - releasedLQ
        else lqAllocSum
      Right(
        (
          copy(
            reserves = reserves.copy(
              LQ  = reserves.LQ - bundle.vLQ,
              vLQ = reserves.vLQ + bundle.vLQ,
              TMP = reserves.TMP + bundle.TMP
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
  val MaxCapTMP: Long = Long.MaxValue

  sealed trait LMPoolErr
  case object ProgramEnded extends LMPoolErr
  case object PrevEpochNotWithdrawn extends LMPoolErr
  case object EpochAlreadyWithdrawn extends LMPoolErr
  case object IllegalEpoch extends LMPoolErr

  type VerifiedST[+A] = Either[LMPoolErr, A]

  val MinCollateralErg = 5000000L
  val DefaultCreationHeight = 100

  implicit def toLedger[F[_]: RuntimeState]: ToLedger[LMPool[F], F] =
    (pool: LMPool[F]) =>
      new LqMiningPoolBox[F](
        boxId("lqm_box_1"),
        MinCollateralErg,
        DefaultCreationHeight,
        tokens = Coll(
          tokenId("nft") -> 1L,
          tokenId("x")   -> pool.reserves.X,
          tokenId("lq")  -> pool.reserves.LQ,
          tokenId("vql") -> pool.reserves.vLQ,
          tokenId("TMP") -> pool.reserves.TMP
        ),
        registers = Map(
          4 -> Coll(
            pool.conf.frameLen,
            pool.conf.epochLen,
            pool.conf.epochNum,
            pool.conf.programStart
          ),
          5 -> BigInt(pool.lqAllocSum),
          6 -> pool.lastUpdatedAtFrameIx,
          7 -> pool.lastUpdatedAtEpochIx,
          8 -> pool.conf.programBudget
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
      PoolReserves(programBudget, 0L, MaxCapVLQ, MaxCapTMP),
      lqAllocSum           = 0L,
      lastUpdatedAtFrameIx = 0,
      lastUpdatedAtEpochIx = 0
    )
}
