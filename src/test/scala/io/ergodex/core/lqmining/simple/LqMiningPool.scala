package io.ergodex.core.lqmining.simple

import cats.kernel.Monoid
import io.ergodex.core.Helpers.{boxId, bytes}
import io.ergodex.core.lqmining.simple.LMPool.MaxCapTMP
import io.ergodex.core.syntax.Coll
import io.ergodex.core.{RuntimeCtx, RuntimeState, ToLedger}

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
      def empty: AssetOutput[T] = AssetOutput(0L)

      def combine(x: AssetOutput[T], y: AssetOutput[T]): AssetOutput[T] = AssetOutput(x.value + y.value)
    }
}

final case class BurnAsset[T](value: Long)

final case class LMConfig(
  epochLen: Int,
  epochNum: Int,
  programStart: Int,
  redeemLimitDelta: Int,
  programBudget: Long,
  maxRoundingError: Long
) {
  val programEnd: Int  = programStart + epochNum * epochLen
  val epochAlloc: Long = (programBudget - 1L) / epochNum
}

final case class PoolReserves(value: Long, X: Long, LQ: Long, vLQ: Long, TMP: Long) {
  val emissionTMP: Long = MaxCapTMP - TMP
}

final case class PoolExecution(execBudget: Long)

final case class StakingBundle(vLQ: Long, TMP: Long)

object StakingBundle {
  implicit val monoid: Monoid[StakingBundle] =
    new Monoid[StakingBundle] {
      def empty: StakingBundle = StakingBundle(0L, 0L)

      def combine(x: StakingBundle, y: StakingBundle): StakingBundle = StakingBundle(x.vLQ + y.vLQ, x.TMP + y.TMP)
    }
}

final case class LMPool[Ledger[_]: RuntimeState](
  conf: LMConfig,
  reserves: PoolReserves,
  execution: PoolExecution,
  lastUpdatedAtEpochIx: Int
) {

  import LMPool._

  def updateReserves(fn: PoolReserves => PoolReserves): LMPool[Ledger] =
    copy(reserves = fn(reserves))

  private def epochIx(ctx: RuntimeCtx): Int = {
    val curBlockIx    = ctx.height - conf.programStart + 1
    val curEpochIxRem = curBlockIx % conf.epochLen
    val curEpochIxR   = curBlockIx / conf.epochLen
    val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR
    curEpochIx
  }

  def deposit(lq: AssetInput[Token.LQ]): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    RuntimeState.withRuntimeState { ctx =>
      if (ctx.height < conf.programEnd) {
        val curEpochIx        = epochIx(ctx)
        val releasedVLQ       = lq.value
        val expectedNumEpochs = conf.epochNum - math.max(0, curEpochIx)
        val releasedTMP       = releasedVLQ * expectedNumEpochs

        val epochIx_ =
          if (curEpochIx != lastUpdatedAtEpochIx) {
            curEpochIx
          } else {
            lastUpdatedAtEpochIx
          }

        val curEpochToCalc = if (curEpochIx <= conf.epochNum) curEpochIx else conf.epochNum + 1

        val prevEpochsCompounded =
          (conf.programBudget - reserves.X) + conf.maxRoundingError >= (curEpochToCalc - 1) * conf.epochAlloc

        if (prevEpochsCompounded) {
          Right(
            copy(
              reserves = reserves.copy(
                LQ  = reserves.LQ + lq.value,
                vLQ = reserves.vLQ - releasedVLQ,
                TMP = reserves.TMP - releasedTMP
              ),
              lastUpdatedAtEpochIx = epochIx_
            ) ->
            StakingBundle(releasedVLQ, releasedTMP)
          )
        } else Left(PrevEpochNotWithdrawn)
      } else Left(ProgramEnded)
    }

  def compound(
    bundle: StakingBundle,
    epoch: Int
  ): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle, AssetOutput[Token.X])]] =
    RuntimeState.withRuntimeState { ctx =>
      val curEpochIx       = epochIx(ctx)
      val epochsToCompound = conf.epochNum - epoch

      if (epoch <= curEpochIx - 1) {
        if (reserves.X - epochsToCompound * conf.epochAlloc <= conf.epochAlloc + conf.maxRoundingError) {

          val revokedTMP   = bundle.TMP - epochsToCompound * bundle.vLQ
          val epochsBurned = (bundle.TMP / bundle.vLQ) - epochsToCompound
          val reward = if (revokedTMP > 0) {
            (BigInt(conf.epochAlloc) * bundle.vLQ * epochsBurned / (reserves.LQ - 1L)).toLong
          } else 0L

          val execFee = (BigInt(execution.execBudget) * reward / conf.programBudget).toLong
          Right(
            (
              updateReserves(r =>
                PoolReserves(value = r.value - execFee, X = r.X - reward, r.LQ, r.vLQ, TMP = r.TMP + revokedTMP)
              ),
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
      val curEpochIx     = if (ctx.height < conf.programEnd) epochIx(ctx) else conf.epochNum + 1
      val releasedLQ     = bundle.vLQ
      val curEpochToCalc = if (curEpochIx <= conf.epochNum) curEpochIx else conf.epochNum + 1

      val prevEpochsCompoundedForRedeem =
        (conf.programBudget - reserves.X) + conf.maxRoundingError >= (curEpochToCalc - 1) * conf.epochAlloc

      val redeemNoLimit = ctx.height >= conf.programStart + conf.epochNum * conf.epochLen + conf.redeemLimitDelta

      if (prevEpochsCompoundedForRedeem || redeemNoLimit) {
        Right(
          (
            copy(
              reserves = reserves.copy(
                LQ  = reserves.LQ - bundle.vLQ,
                vLQ = reserves.vLQ + bundle.vLQ,
                TMP = reserves.TMP + bundle.TMP
              )
            ),
            AssetOutput(releasedLQ)
          )
        )
      } else Left(PrevEpochNotWithdrawn)
    }

  def increaseExecutionBudget(valueAdd: Long): Ledger[VerifiedST[LMPool[Ledger]]] =
    RuntimeState.withRuntimeState { ctx =>
      Right(
        copy(
          reserves = reserves.copy(
            value = reserves.value + valueAdd
          )
        )
      )
    }
}

object LMPool {
  val MaxCapVLQ: Long = Long.MaxValue
  val MaxCapTMP: Long = Long.MaxValue

  sealed trait LMPoolErr

  case object ProgramEnded extends LMPoolErr

  case object ProgramNotEnded extends LMPoolErr

  case object PrevEpochNotWithdrawn extends LMPoolErr

  case object EpochAlreadyWithdrawn extends LMPoolErr

  case object IllegalEpoch extends LMPoolErr

  type VerifiedST[+A] = Either[LMPoolErr, A]

  val MinCollateralErg           = 5000000L
  val DefaultCreationHeight      = 1000
  val BundleKeyTokenAmount: Long = 0x7fffffffffffffffL - 1L

  implicit def toLedger[F[_]: RuntimeState]: ToLedger[LMPool[F], F] = { (pool: LMPool[F]) =>
    val programBudgetCorrected = pool.conf.programBudget - 1L

    val tokensNew = {
      if (pool.reserves.X == 0 && pool.reserves.LQ == 0)
        Coll(
          bytes("LM_Pool_NFT_ID") -> 1L,
          bytes("LQ")             -> pool.reserves.LQ,
          bytes("TMP")            -> pool.reserves.TMP
        )
      else if (pool.reserves.X == 0 && pool.reserves.LQ > 0)
        Coll(
          bytes("LM_Pool_NFT_ID") -> 1L,
          bytes("LQ")             -> pool.reserves.LQ,
          bytes("vLQ")            -> pool.reserves.vLQ,
          bytes("TMP")            -> pool.reserves.TMP
        )
      else if (pool.reserves.X > 0 && pool.reserves.LQ == 0)
        Coll(
          bytes("LM_Pool_NFT_ID") -> 1L,
          bytes("X")              -> pool.reserves.X,
          bytes("vLQ")            -> pool.reserves.vLQ,
          bytes("TMP")            -> pool.reserves.TMP
        )
      else
        Coll(
          bytes("LM_Pool_NFT_ID") -> 1L,
          bytes("X")              -> pool.reserves.X,
          bytes("LQ")             -> pool.reserves.LQ,
          bytes("vLQ")            -> pool.reserves.vLQ,
          bytes("TMP")            -> pool.reserves.TMP
        )
    }

    new LqMiningPoolBox[F](
      boxId("lm_pool_id"),
      pool.reserves.value,
      DefaultCreationHeight,
      tokens = tokensNew,
      registers = Map(
        4 -> Coll(
          pool.conf.epochLen,
          pool.conf.epochNum,
          pool.conf.programStart,
          pool.conf.redeemLimitDelta
        ),
        5 -> programBudgetCorrected,
        6 -> pool.conf.maxRoundingError,
        7 -> pool.execution.execBudget
      )
    )
  }

  def init[Ledger[_]: RuntimeState](
    epochLen: Int,
    epochNum: Int,
    programStart: Int,
    redeemLimitDelta: Int,
    programBudget: Long,
    maxRoundingError: Long
  ): LMPool[Ledger] =
    LMPool(
      LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, programBudget, maxRoundingError),
      PoolReserves(MinCollateralErg, programBudget, 1L, MaxCapVLQ, MaxCapTMP),
      PoolExecution(MinCollateralErg),
      lastUpdatedAtEpochIx = 0
    )
}
