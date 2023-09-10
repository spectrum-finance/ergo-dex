package io.ergodex.core.lqmining.extended

import cats.kernel.Monoid
import io.ergodex.core.Helpers.{boxId, bytes}
import io.ergodex.core.lqmining.extended.LMPool.MaxCapTMP
import io.ergodex.core.syntax.Coll
import io.ergodex.core.{RuntimeCtx, RuntimeState, ToLedger}

object Token {
  type X0
  type X1
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
  maxRoundingError: Long
) {
  val programEnd: Int = programStart + epochNum * epochLen
}

final case class PoolRedeemers(mainBudgetRedeemer: Coll[Byte], optBudgetRedeemer: Coll[Byte])
final case class ActualBudgets(mainBudget: Long, optBudget: Long)

final case class PoolReserves(value: Long, X0: Long, LQ: Long, vLQ: Long, TMP: Long, X1: Long) {
  val supplyTMP: Long = MaxCapTMP - TMP
}

final case class StakingBundle(vLQ: Long, TMP: Long, rewardMain: Long, rewardOpt: Long)

object StakingBundle {
  implicit val monoid: Monoid[StakingBundle] =
    new Monoid[StakingBundle] {
      def empty: StakingBundle = StakingBundle(0L, 0L, 0L, 0L)

      def combine(x: StakingBundle, y: StakingBundle): StakingBundle =
        StakingBundle(x.vLQ + y.vLQ, x.TMP + y.TMP, x.rewardMain + y.rewardMain, x.rewardOpt + y.rewardOpt)
    }
}

final case class LMPool[Ledger[_]: RuntimeState](
  conf: LMConfig,
  redeemers: PoolRedeemers,
  budgets: ActualBudgets,
  reserves: PoolReserves,
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

        val prevEpochsCompoundedForDeposit = true

        // prevEpochsCompoundedForDeposit is true above to check in contract the condition:
        // val curEpochIx     = if (ctx.height < conf.programEnd) epochIx(ctx) else conf.epochNum + 1
        // val curEpochToCalc = if (curEpochIx <= conf.epochNum) curEpochIx else conf.epochNum + 1
        // val prevEpochsCompoundedForDeposit =
        // ((conf.programBudget - reserves.X) + conf.maxRoundingError) >= (curEpochToCalc - 1) * conf.epochAlloc

        val epochIx_ =
          if (curEpochIx != lastUpdatedAtEpochIx) {
            curEpochIx
          } else {
            lastUpdatedAtEpochIx
          }
        if (prevEpochsCompoundedForDeposit) {
          Right(
            copy(
              reserves = reserves.copy(
                LQ  = reserves.LQ + lq.value,
                vLQ = reserves.vLQ - releasedVLQ,
                TMP = reserves.TMP - releasedTMP
              ),
              lastUpdatedAtEpochIx = epochIx_
            ) ->
            StakingBundle(releasedVLQ, releasedTMP, 0L, 0L)
          )
        } else Left(PrevEpochNotWithdrawn)
      } else Left(ProgramEnded)
    }

  def compound(
    bundle: StakingBundle,
    epoch: Int
  ): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    RuntimeState.withRuntimeState { ctx =>
      val curEpochIx       = epochIx(ctx)
      val epochsToCompound = conf.epochNum - epoch
      val epochNumToEnd    = epochsToCompound + 1
      val epochMainAlloc   = budgets.mainBudget / epochNumToEnd
      val epochOptAlloc    = budgets.optBudget / epochNumToEnd

      val prevEpochsCompounded =
        BigInt(reserves.X0) + conf.maxRoundingError >= epochsToCompound * epochMainAlloc &&
        BigInt(reserves.X1) + conf.maxRoundingError >= epochsToCompound * epochOptAlloc

      if (epoch <= curEpochIx - 1) {
        if (prevEpochsCompounded) {
          val revokedTMP   = bundle.TMP - epochsToCompound * bundle.vLQ
          val epochsBurned = (bundle.TMP / bundle.vLQ) - epochsToCompound

          val actualTMP    = reserves.supplyTMP - reserves.LQ * epochsToCompound
          val allocMainRem = reserves.X0 - epochMainAlloc * epochsToCompound
          val allocOptRem  = reserves.X1 - epochOptAlloc * epochsToCompound

          val rewardMain =
            if (actualTMP > 0 && epochsBurned > 0) ((allocMainRem * bundle.vLQ * epochsBurned) / actualTMP).toLong
            else 0L
          val rewardOpt =
            if (actualTMP > 0 && epochsBurned > 0) ((allocOptRem * bundle.vLQ * epochsBurned) / actualTMP).toLong
            else 0L

          Right(
            (
              updateReserves(r =>
                PoolReserves(
                  value = r.value,
                  X0    = r.X0 - rewardMain,
                  r.LQ,
                  r.vLQ,
                  TMP = r.TMP + revokedTMP,
                  X1  = r.X1 - rewardOpt
                )
              ),
              bundle.copy(
                TMP        = bundle.TMP - revokedTMP,
                rewardMain = bundle.rewardMain + rewardMain,
                rewardOpt  = bundle.rewardOpt + rewardOpt
              )
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
      val releasedLQ     = bundle.vLQ
      val curEpochIx     = epochIx(ctx)
      val curEpochToCalc = if (curEpochIx <= conf.epochNum) curEpochIx else conf.epochNum + 1
      val epochNumToEnd  = if (curEpochIx < conf.epochNum) conf.epochNum - curEpochToCalc + 1 else 0
      val epochMainAlloc = budgets.mainBudget / epochNumToEnd
      val epochOptAlloc  = budgets.optBudget / epochNumToEnd

      val prevEpochsCompounded =
        BigInt(reserves.X0) + conf.maxRoundingError >= epochNumToEnd * epochMainAlloc &&
        BigInt(reserves.X1) + conf.maxRoundingError >= epochNumToEnd * epochOptAlloc

      val redeemNoLimit = ctx.height >= conf.programStart + conf.epochNum * conf.epochLen + conf.redeemLimitDelta

      if (prevEpochsCompounded || redeemNoLimit) {

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

  def redeemBudget(
    redeemedX0: Long,
    redeemedX1: Long
  ): Ledger[VerifiedST[(LMPool[Ledger], AssetOutput[Token.X0], AssetOutput[Token.X1])]] =
    RuntimeState.withRuntimeState { ctx =>
      val redeemNoLimit = ctx.height >= conf.programStart + conf.epochNum * conf.epochLen + conf.redeemLimitDelta

      if (redeemNoLimit) {

        Right(
          (
            copy(
              reserves = reserves.copy(
                X0 = reserves.X0 - redeemedX0,
                X1 = reserves.X0 - redeemedX1
              )
            ),
            AssetOutput(redeemedX0),
            AssetOutput(redeemedX0)
          )
        )
      } else Left(ProgramNotEnded)
    }

  def depositBudget(valueAdd: Long, budgetId: Int): Ledger[VerifiedST[LMPool[Ledger]]] =
    RuntimeState.withRuntimeState { _ =>
      if (budgetId == 0)
        Right(
          copy(
            reserves = reserves.copy(
              X0 = reserves.X0 + valueAdd
            )
          )
        )
      else
        Right(
          copy(
            reserves = reserves.copy(
              X1 = reserves.X1 + valueAdd
            )
          )
        )
    }

  def updateBudgets(): Ledger[VerifiedST[LMPool[Ledger]]] =
    RuntimeState.withRuntimeState { ctx =>
      val curEpochIx     = epochIx(ctx)
      val curEpochToCalc = if (curEpochIx <= conf.epochNum) curEpochIx else conf.epochNum + 1
      val epochNumToEnd  = if (curEpochIx < conf.epochNum) conf.epochNum - curEpochToCalc + 1 else 0
      val epochMainAlloc = budgets.mainBudget / epochNumToEnd
      val epochOptAlloc  = budgets.optBudget / epochNumToEnd

      val prevEpochsCompounded =
        BigInt(reserves.X0) + conf.maxRoundingError >= epochNumToEnd * epochMainAlloc &&
        BigInt(reserves.X1) + conf.maxRoundingError >= epochNumToEnd * epochOptAlloc
      if (prevEpochsCompounded) {
        Right(
          copy(
            budgets = budgets.copy(mainBudget = reserves.X0, optBudget = reserves.X1)
          )
        )
      } else Left(PrevEpochNotWithdrawn)
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
    val tokensNew =
      Coll(
        bytes("LM_Pool_NFT_ID") -> 1L,
        bytes("X0")             -> pool.reserves.X0,
        bytes("LQ")             -> pool.reserves.LQ,
        bytes("vLQ")            -> pool.reserves.vLQ,
        bytes("TMP")            -> pool.reserves.TMP,
        bytes("X1")             -> pool.reserves.X1
      )
    val mainBudgetCorrected = pool.budgets.mainBudget - 1L
    val optBudgetCorrected  = pool.budgets.mainBudget - 1L
    new LqMiningPoolBoxSelfHosted[F](
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
        5 -> Coll(
          pool.budgets.mainBudget,
          pool.budgets.optBudget
        ),
        6 -> Coll(
          mainBudgetCorrected,
          optBudgetCorrected
        ),
        7 -> pool.conf.maxRoundingError
      )
    )
  }

  def init[Ledger[_]: RuntimeState](
    epochLen: Int,
    epochNum: Int,
    mainBudget: Long,
    optBudget: Long,
    programStart: Int,
    redeemLimitDelta: Int,
    maxRoundingError: Long
  ): LMPool[Ledger] =
    LMPool(
      conf = LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, maxRoundingError),
      PoolRedeemers(bytes("Host"), bytes("Spectrum")),
      ActualBudgets(mainBudget, optBudget),
      PoolReserves(MinCollateralErg, mainBudget, 0L, MaxCapVLQ, MaxCapTMP, optBudget),
      lastUpdatedAtEpochIx = 0
    )
}
