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

final case class LMPool[Ledger[_]: LedgerState](conf: LMConfig, reserves: PoolReserves, totalStakes: Long) {
  import LMPool._

  def updateReserves(fn: PoolReserves => PoolReserves): LMPool[Ledger] =
    copy(reserves = fn(reserves))

  def deposit(lq: AssetInput[Token.LQ]): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    LedgerState.withLedgerState { ctx =>
      if (ctx.height < conf.programEnd) {
        val curFrame    = (ctx.height - conf.programStart + 1) / conf.frameLen
        val releasedVLQ = lq.value
        val releasedTT  = conf.framesNum - math.max(0, curFrame)
        Right(
          copy(
            reserves = reserves.copy(
              LQ  = reserves.LQ + lq.value,
              vLQ = reserves.vLQ - releasedVLQ,
              TT  = reserves.TT - releasedTT
            ),
            totalStakes = totalStakes + 1
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
        if (reserves.X - epochsToCompound * conf.epochAlloc <= conf.epochAlloc) {
          // val epochTT = reserves.emissionTT - (conf.epochLen * epochsToCompound * fullEpochsLeft) // todo: track burned TT tokens?
          val epochTT      = reserves.emissionTT - (conf.epochLen * (conf.epochNum - 1) * totalStakes)
          val inputEpochTT = bundle.TT - conf.epochLen * epochsToCompound
          val reward =
            (BigInt(bundle.vLQ) * inputEpochTT * conf.epochAlloc /
              (BigInt(reserves.LQ) * epochTT / totalStakes)).toLong
          Right(
            (
              updateReserves(r => PoolReserves(X = r.X - reward, r.LQ, r.vLQ, TT = r.TT)),
              bundle.copy(TT = bundle.TT - inputEpochTT),
              AssetOutput(reward),
              BurnAsset(inputEpochTT)
            )
          )
        } else Left(PrevEpochNotWithdrawn)
      } else Left(IllegalEpoch)
    }

  def redeem(bundle: StakingBundle): Ledger[VerifiedST[(LMPool[Ledger], AssetOutput[Token.LQ])]] =
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
      totalStakes = 0L
    )
}
