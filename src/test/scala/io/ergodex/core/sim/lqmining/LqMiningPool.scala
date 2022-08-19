package io.ergodex.core.sim.lqmining

import io.ergodex.core.sim.{LedgerCtx, LedgerState}
import io.ergodex.core.sim.lqmining.LMPool.MaxCapTT

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

object Token {
  type X
  type LQ
  type TT
}

final case class AssetInput[T](value: Long)
final case class AssetOutput[T](value: Long)
final case class BurnAsset[T](value: Long)

final case class LMPool[Ledger[_]: LedgerState](
  conf: LMConfig,
  reserves: PoolReserves,
  vLQAllocated: Long,
  lastUpdatedAtFrameIx: Int,
  lastUpdatedAtEpochIx: Int
) {
  import LMPool._

  def updateReserves(fn: PoolReserves => PoolReserves): LMPool[Ledger] =
    copy(reserves = fn(reserves))

  def deposit(lq: AssetInput[Token.LQ]): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    LedgerState.withLedgerState { ctx =>
      if (ctx.height < conf.programEnd) {
        val curFrameIx  = (ctx.height - conf.programStart + 1) / conf.frameLen
        val curEpochIx  = curFrameIx / conf.epochLen
        val releasedVLQ = lq.value
        val releasedTT  = conf.framesNum - math.max(0, curFrameIx)
        val (vLQAllocated_, frameIx_, epochIx_) =
          if (ctx.height < conf.programStart) // todo: do not allow further deposits until last epoch is compounded
            (vLQAllocated, lastUpdatedAtFrameIx, lastUpdatedAtEpochIx)
          else if (curEpochIx != lastUpdatedAtEpochIx) {
            val passedFrames = curFrameIx - (curEpochIx - 1) * conf.epochLen
            (passedFrames * reserves.LQ, curFrameIx, curFrameIx)
          } else if (curFrameIx != lastUpdatedAtFrameIx) {
            val passedFrames = curFrameIx - lastUpdatedAtFrameIx
            println(passedFrames)
            (vLQAllocated + passedFrames * reserves.LQ, curFrameIx, lastUpdatedAtEpochIx)
          } else
            (vLQAllocated, lastUpdatedAtFrameIx, lastUpdatedAtEpochIx)
        Right(
          copy(
            reserves = reserves.copy(
              LQ  = reserves.LQ + lq.value,
              vLQ = reserves.vLQ - releasedVLQ,
              TT  = reserves.TT - releasedTT
            ),
            vLQAllocated         = vLQAllocated_,
            lastUpdatedAtFrameIx = frameIx_,
            lastUpdatedAtEpochIx = epochIx_
          ) ->
          StakingBundle(releasedVLQ, releasedTT.toLong)
        )
      } else Left(ProgramEnded)
    }

  def compound(
    bundle: StakingBundle,
    epoch: Int
  ): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle, AssetOutput[Token.X], BurnAsset[Token.TT])]] =
    LedgerState.withLedgerState { ctx =>
      val curEpoch         = (ctx.height - conf.programStart + 1) / (conf.frameLen * conf.epochLen)
      val epochsToCompound = conf.epochNum - epoch
      if (epoch <= curEpoch - 1) {
        val inputEpochTT =
          bundle.TT - conf.epochLen * epochsToCompound // todo: bundle.TT - conf.epochLen * (epochsToCompound-1) ?
        val vLQAllocated_   = if (lastUpdatedAtEpochIx != curEpoch) reserves.LQ else vLQAllocated
        val vLQAllocatedAvg = vLQAllocated_ / conf.epochLen
        println((conf.epochAlloc, bundle.vLQ, vLQAllocatedAvg, inputEpochTT, conf.epochLen))
        val reward = conf.epochAlloc * bundle.vLQ / vLQAllocatedAvg * inputEpochTT / conf.epochLen
//          (BigInt(bundle.vLQ) * conf.epochAlloc * inputEpochTT / (BigInt(
//            reserves.LQ
//          ) * conf.epochLen)).toLong // todo: handle depositing half-way
        Right(
          (
            updateReserves(r => PoolReserves(X = r.X - reward, r.LQ, r.vLQ, TT = r.TT)),
            bundle.copy(TT = bundle.TT - inputEpochTT),
            AssetOutput(reward),
            BurnAsset(inputEpochTT)
          )
        )
      } else Left(IllegalEpoch)
    }

  def redeem(bundle: StakingBundle): Ledger[VerifiedST[(LMPool[Ledger], AssetOutput[Token.LQ])]] = // todo
    LedgerState.withLedgerState { _ =>
      val releasedLQ = bundle.vLQ
      Right(
        copy(
          reserves = reserves.copy(
            LQ  = reserves.LQ - bundle.vLQ,
            vLQ = reserves.vLQ + bundle.vLQ,
            TT  = reserves.TT + bundle.TT
          ),
          vLQAllocated = vLQAllocated - 1
        ) ->
        AssetOutput(releasedLQ)
      )
    }

  def logState(ctx: LedgerCtx): Unit = {
    val epoch      = (ctx.height - conf.programStart + 1) / (conf.frameLen * conf.epochLen)
    val epochsLeft = conf.epochNum - epoch
    println(s"""
        |RESERVES_X:   ${reserves.X}
        |RESERVES_LQ:  ${reserves.LQ}
        |RESERVES_vLQ: ${reserves.vLQ}
        |RESERVES_TT:  ${reserves.TT}
        |
        |STAKES_TOTAL: $vLQAllocated
        |
        |EPOCH_ALLOC: ${conf.epochAlloc}
        |
        |EPOCH: $epoch
        |EPOCHS_LEFT: $epochsLeft
        |""".stripMargin)
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

  def init[Ledger[_]: LedgerState](
    frameLen: Int,
    epochLen: Int,
    epochNum: Int,
    programStart: Int,
    programBudget: Long
  ): LMPool[Ledger] =
    LMPool(
      LMConfig(frameLen, epochLen, epochNum, programStart, programBudget),
      PoolReserves(programBudget, 0L, MaxCapVLQ, MaxCapTT),
      vLQAllocated         = 0L,
      lastUpdatedAtFrameIx = 0,
      lastUpdatedAtEpochIx = 0
    )
}
