package io.ergodex.core.sim

import io.ergodex.core.sim.LMPool.MaxCapTT

final case class LMConfig(frameLen: Int, epochLen: Int, epochNum: Int, programStart: Int, programBudget: Long) {
  val programEnd: Int  = programStart + frameLen * epochNum * epochLen
  val framesNum: Int   = epochNum * epochLen
  val epochAlloc: Long = programBudget / epochNum
}

final case class PoolReserves(X: Long, LQ: Long, vLQ: Long, TT: Long) {
  val emissionTT: Long = MaxCapTT - TT
}

final case class StakingBundle(vlq: Long, tt: Long)

final case class Reward(x: Long)

final case class LMPool[Ledger[_]: LedgerState](conf: LMConfig, reserves: PoolReserves) {
  import LMPool._

  def updateReserves(fn: PoolReserves => PoolReserves): LMPool[Ledger] =
    copy(reserves = fn(reserves))

  def deposit(lq: Long): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    LedgerState.withLedgerState { ctx =>
      if (ctx.height >= conf.programStart && ctx.height < conf.programEnd) {
        val frame       = (ctx.height - conf.programStart) / conf.frameLen
        val releasedVLQ = lq
        val releasedTT  = conf.framesNum - frame
        Right(
          updateReserves(r => PoolReserves(r.X, LQ = r.LQ + lq, vLQ = r.vLQ - releasedVLQ, TT = r.TT - releasedTT)) ->
          StakingBundle(releasedVLQ, releasedTT)
        )
      } else Left(ProgramInactive)
    }

  def compound(bundle: StakingBundle): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle, Reward)]] =
    LedgerState.withLedgerState { ctx =>
      if (ctx.height >= conf.programStart && ctx.height < conf.programEnd) {
        val epoch      = (ctx.height - conf.programStart) / (conf.frameLen * conf.epochLen)
        val epochsLeft = conf.epochNum - epoch
        if (epochsLeft * conf.epochAlloc == reserves.X) {
          val epochTT      = reserves.emissionTT - conf.epochAlloc * epochsLeft
          val inputEpochTT = bundle.tt - conf.epochLen * epochsLeft
          val reward       = conf.epochAlloc * bundle.vlq * inputEpochTT / (reserves.LQ * epochTT)
          Right(
            (
              updateReserves(r => PoolReserves(X = r.X - reward, r.LQ, r.vLQ, TT = r.TT + inputEpochTT)),
              bundle.copy(tt = bundle.tt - inputEpochTT),
              Reward(reward)
            )
          )
        } else Left(PrevEpochNotWithdrawn)
      } else Left(ProgramInactive)
    }
}

object LMPool {
  val MaxCapVLQ: Long = Long.MaxValue
  val MaxCapTT: Long  = Long.MaxValue

  sealed trait LMPoolErr
  case object ProgramInactive extends LMPoolErr
  case object PrevEpochNotWithdrawn extends LMPoolErr

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
      PoolReserves(programBudget, 0L, MaxCapVLQ, MaxCapTT)
    )
}
