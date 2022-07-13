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
}

final case class Asset[T](value: Long)

final case class LMPool[Ledger[_]: LedgerState](conf: LMConfig, reserves: PoolReserves, totalStakes: Long) {
  import LMPool._

  def updateReserves(fn: PoolReserves => PoolReserves): LMPool[Ledger] =
    copy(reserves = fn(reserves))

  def deposit(lq: Asset[Token.LQ]): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    LedgerState.withLedgerState { ctx =>
      if (ctx.height >= conf.programStart && ctx.height < conf.programEnd) {
        val frame       = (ctx.height - conf.programStart) / conf.frameLen
        val releasedVLQ = lq.value
        val releasedTT  = conf.framesNum - frame
        Right(
          copy(
            reserves = reserves.copy(
              LQ  = reserves.LQ + lq.value,
              vLQ = reserves.vLQ - releasedVLQ,
              TT  = reserves.TT - releasedTT
            ),
            totalStakes = totalStakes + 1
          ) ->
          StakingBundle(releasedVLQ, releasedTT)
        )
      } else Left(ProgramInactive)
    }

  def compound(bundle: StakingBundle): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle, Asset[Token.X])]] =
    LedgerState.withLedgerState { ctx =>
      if (ctx.height >= conf.programStart && ctx.height < conf.programEnd) {
        val curEpoch            = (ctx.height - conf.programStart + 1) / (conf.frameLen * conf.epochLen)
        val fullEpochsLeft   = conf.epochNum - curEpoch
        val epochsToCompound = fullEpochsLeft + 1
        logState(ctx)
        println(reserves.X - epochsToCompound * conf.epochAlloc)
        if (reserves.X - epochsToCompound * conf.epochAlloc <= conf.epochAlloc) {
          val epochTT      = reserves.emissionTT - (conf.epochLen * epochsToCompound * fullEpochsLeft)
          val inputEpochTT = bundle.TT - conf.epochLen * epochsToCompound
          if (epochTT > 0) {
            val reward = conf.epochAlloc * bundle.vLQ * inputEpochTT / (reserves.LQ * epochTT)
            Right(
              (
                updateReserves(r => PoolReserves(X = r.X - reward, r.LQ, r.vLQ, TT = r.TT + inputEpochTT)),
                bundle.copy(TT = bundle.TT - inputEpochTT),
                Asset(reward)
              )
            )
          }
          else Left(EpochAlreadyWithdrawn)
        } else Left(PrevEpochNotWithdrawn)
      } else Left(ProgramInactive)
    }

  def redeem(bundle: StakingBundle): Ledger[VerifiedST[(LMPool[Ledger], Asset[Token.LQ])]] =
    LedgerState.withLedgerState { _ =>
      val releasedLQ = bundle.vLQ
      Right(
        copy(
          reserves = reserves.copy(
            LQ  = reserves.LQ - bundle.vLQ,
            vLQ = reserves.vLQ + bundle.vLQ,
            TT  = reserves.TT + bundle.TT
          ),
          totalStakes = totalStakes - 1
        ) ->
        Asset(releasedLQ)
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
        |STAKES_TOTAL: $totalStakes
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
  case object ProgramInactive extends LMPoolErr
  case object PrevEpochNotWithdrawn extends LMPoolErr
  case object EpochAlreadyWithdrawn extends LMPoolErr

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
      totalStakes = 0L
    )
}
