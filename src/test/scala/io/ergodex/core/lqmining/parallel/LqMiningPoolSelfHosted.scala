package io.ergodex.core.lqmining.parallel

import cats.kernel.Monoid
import io.ergodex.core.Helpers.{boxId, bytes}
import io.ergodex.core.lqmining.parallel.LMPool.MaxCapTMP
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
  maxRoundingError: Long,
  mainBudget: Long,
  optBudget: Long
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

  private def epochIx(ctx: RuntimeCtx): (Int, Int) = {
    val curBlockIx    = ctx.height - conf.programStart + 1
    val curEpochIxRem = curBlockIx % conf.epochLen
    val curEpochIxR   = curBlockIx / conf.epochLen
    val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR
    (curEpochIx, curEpochIxR)
  }

  def deposit(lq: AssetInput[Token.LQ]): Ledger[VerifiedST[(LMPool[Ledger], StakingBundle)]] =
    RuntimeState.withRuntimeState { ctx =>
      if (ctx.height < conf.programEnd) {
        val (curEpochIx, _)   = epochIx(ctx)
        val releasedVLQ       = lq.value
        val expectedNumEpochs = conf.epochNum - math.max(0, curEpochIx)
        val releasedTMP       = releasedVLQ * expectedNumEpochs

        val prevEpochsCompoundedForDeposit = true

        if (prevEpochsCompoundedForDeposit) {
          Right(
            copy(
              reserves = reserves.copy(
                LQ  = reserves.LQ + lq.value,
                vLQ = reserves.vLQ - releasedVLQ,
                TMP = reserves.TMP - releasedTMP
              )
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
      val (curEpochIx, _)  = epochIx(ctx)
      val epochsToCompound = conf.epochNum - epoch
      val epochNumToEnd    = epochsToCompound + 1
      val epochMainAlloc   = budgets.mainBudget / epochNumToEnd
      val epochOptAlloc    = budgets.optBudget / epochNumToEnd

      val prevEpochsCompounded = true
      if (epoch <= curEpochIx - 1) {
        if (prevEpochsCompounded) {
          val revokedTMP   = bundle.TMP - epochsToCompound * bundle.vLQ
          val epochsBurned = (bundle.TMP / bundle.vLQ) - epochsToCompound

          val actualTMP    = reserves.supplyTMP - reserves.LQ * epochsToCompound
          val allocMainRem = reserves.X0 - epochMainAlloc * epochsToCompound
          val allocOptRem  = reserves.X1 - epochOptAlloc * epochsToCompound

          val rewardMain =
            if (actualTMP > 0 && epochsBurned > 0)
              (((BigInt(allocMainRem) - 1L) * bundle.vLQ * epochsBurned) / actualTMP).toLong
            else 0L
          val rewardOpt =
            if (actualTMP > 0 && epochsBurned > 0)
              (((BigInt(allocOptRem) - 1L) * bundle.vLQ * epochsBurned) / actualTMP).toLong
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
    RuntimeState.withRuntimeState { _ =>
      val releasedLQ = bundle.vLQ

      val prevEpochsCompounded = true
      val redeemNoLimit        = true

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

  def updateBudgets(compounded_epoch: Int): Ledger[VerifiedST[LMPool[Ledger]]] =
    RuntimeState.withRuntimeState { ctx =>
      val prevEpochsCompounded = true

      if (prevEpochsCompounded) {
        Right(
          copy(
            budgets              = budgets.copy(mainBudget = reserves.X0, optBudget = reserves.X1),
            lastUpdatedAtEpochIx = compounded_epoch
          )
        )
      } else Left(PrevEpochNotWithdrawn)
    }

  def depositBudget(valueAdd: Long, budgetId: Int): Ledger[VerifiedST[LMPool[Ledger]]] =
    RuntimeState.withRuntimeState { _ =>
      if (budgetId == 0)
        Right(
          copy(
            reserves = reserves.copy(
              X0 = reserves.X0 + valueAdd
            ),
            budgets = budgets.copy(
              mainBudget = budgets.mainBudget + valueAdd
            )
          )
        )
      else
        Right(
          copy(
            reserves = reserves.copy(
              X1 = reserves.X1 + valueAdd
            ),
            budgets = budgets.copy(
              optBudget = budgets.optBudget + valueAdd
            )
          )
        )
    }
  def redeemBudget(budgetId: Int): Ledger[VerifiedST[(LMPool[Ledger], AssetOutput[Any])]] =
    RuntimeState.withRuntimeState { ctx =>
      val redeemNoLimit = ctx.height >= conf.programStart + conf.epochNum * conf.epochLen + conf.redeemLimitDelta

      if (redeemNoLimit) {
        if (budgetId == 0) {

          Right(
            (
              copy(
                reserves = reserves.copy(
                  X0 = 0
                )
              ),
              AssetOutput(reserves.X0)
            )
          )
        } else {
          Right(
            (
              copy(
                reserves = reserves.copy(
                  X1 = 0
                )
              ),
              AssetOutput(reserves.X1)
            )
          )
        }
      } else Left(ProgramNotEnded)
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
        bytes("VLQ")            -> pool.reserves.vLQ,
        bytes("TMP")            -> pool.reserves.TMP,
        bytes("X1")             -> pool.reserves.X1
      )
    new LqMiningPoolBoxSelfHosted[F](
      boxId("bundle_key_id"),
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
          pool.redeemers.mainBudgetRedeemer,
          pool.redeemers.optBudgetRedeemer
        ),
        7 -> pool.conf.maxRoundingError,
        8 -> pool.lastUpdatedAtEpochIx
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
      conf = LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, maxRoundingError, mainBudget, optBudget),
      PoolRedeemers(bytes("Host"), bytes("Spectrum")),
      ActualBudgets(mainBudget, optBudget),
      PoolReserves(MinCollateralErg, mainBudget, 0L, MaxCapVLQ, MaxCapTMP, optBudget),
      lastUpdatedAtEpochIx = 0
    )
}
