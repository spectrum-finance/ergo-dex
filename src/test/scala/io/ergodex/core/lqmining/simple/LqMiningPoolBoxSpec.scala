package io.ergodex.core.lqmining.simple

import io.ergodex.core.Helpers.{boxId, bytes}
import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.simple.LMPool._
import io.ergodex.core.lqmining.simple.Token._
import io.ergodex.core.lqmining.simple.TxBoxes._
import io.ergodex.core.lqmining.simple.generators.{lmConfGen, DepositGen}
import io.ergodex.core.syntax.Coll
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class LqMiningPoolBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  forAll(lmConfGen) { conf =>
    forAll(DepositGen(1L)) { case lq =>
      val epochLen         = conf.epochLen
      val epochNum         = conf.epochNum
      val programStart     = conf.programStart
      val redeemLimitDelta = conf.redeemLimitDelta
      val programBudget    = conf.programBudget
      val maxRoundingError = conf.maxRoundingError
      val baseAssetAmount  = lq

      val epochReg = 8

      val testId = Random.nextLong()

      val pool01: LMPool[Ledger] =
        LMPool.init(
          epochLen,
          epochNum,
          programStart,
          redeemLimitDelta,
          programBudget,
          maxRoundingError
        )
      val input0: AssetInput[LQ] = AssetInput(1 * baseAssetAmount)
      val input1: AssetInput[LQ] = AssetInput(2 * baseAssetAmount)

      def epochIx(ctx: RuntimeCtx, conf: LMConfig): Int = {
        val curBlockIx    = ctx.height - conf.programStart + 1
        val curEpochIxRem = curBlockIx % conf.epochLen
        val curEpochIxR   = curBlockIx / conf.epochLen
        val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR
        curEpochIx
      }

      it should s"validate deposit behaviour during LM program mirrored from simulation$testId" in {
        val startAtHeight                = programStart + 1
        val action                       = pool01.deposit(input0)
        val currEpoch                    = epochIx(RuntimeCtx.at(startAtHeight), pool01.conf)
        val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox0                     = pool01.toLedger[Ledger]
        val poolBox1                     = pool1.toLedger[Ledger]

        val (userBox1, depositBox1, bundleBox1) =
          getDepositTxBoxes(input0.value, pool01.conf.epochNum - currEpoch, bundle1.vLQ, bundle1.TMP)

        val txInputs  = List(poolBox0)
        val txOutputs = List(poolBox1, userBox1, bundleBox1)

        val (_, isValidDeposit) = depositBox1.validator
          .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
          .value
        val (_, isValidPool) = poolBox0.validator
          .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
          .value

        isValidPool shouldBe true
        isValidDeposit shouldBe true

      }

      it should s"validate deposit behaviour during LM program mirrored from simulation during compounding$testId" in {
        val startAtHeight                  = programStart - 1
        val action                         = pool01.deposit(input0)
        val currEpoch                      = epochIx(RuntimeCtx.at(startAtHeight), pool01.conf)
        val (_, Right((pool1, bundle1)))   = action.run(RuntimeCtx.at(startAtHeight)).value
        val action1                        = pool1.deposit(input0)
        val (_, Right((pool2, bundle12)))  = action1.run(RuntimeCtx.at(startAtHeight)).value
        val action2                        = pool2.compound(bundle1, epoch = 1)
        val (_, Right((pool3, _, reward))) = action2.run(RuntimeCtx.at(startAtHeight + epochLen + 2)).value
        val action3                        = pool3.deposit(input0)
        val (_, Right((pool4, _)))         = action3.run(RuntimeCtx.at(startAtHeight + epochLen + 2)).value

        val poolBox3 = pool3.toLedger[Ledger]
        val poolBox4 = pool4.toLedger[Ledger]

        val (userBox3, depositBox3, bundleBox3) =
          getDepositTxBoxes(input0.value, pool01.conf.epochNum - currEpoch, bundle1.vLQ, bundle1.TMP)

        val txInputs  = List(poolBox3)
        val txOutputs = List(poolBox4, userBox3, bundleBox3)

        val (_, isValidDeposit) = depositBox3.validator
          .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
          .value
        val (_, isValidPool) = poolBox3.validator
          .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
          .value

        math.abs(reward.value - pool3.conf.epochAlloc / 2) <= 1 shouldBe true
        isValidPool shouldBe false
        isValidDeposit shouldBe true
      }

      it should s"validate deposit behaviour after LM program end mirrored from simulation$testId" in {
        val startAtHeight    = programStart + epochNum * epochLen + 1
        val action           = pool01.deposit(input0)
        val (_, Left(pool1)) = action.run(RuntimeCtx.at(startAtHeight)).value
        pool1 shouldBe ProgramEnded
      }

      it should s"validate compound behaviour after first epoch mirrored from simulation$testId" in {
        val startAtHeight = programStart - 1
        val epochStep     = epochLen + 2
        val action = for {
          Right((pool1, sb1))  <- pool01.deposit(input0)
          Right((pool2, sb2))  <- pool1.deposit(input1)
          -                    <- Ledger.extendBy(epochStep)
          Right((pool3, _, _)) <- pool2.compound(sb1, epoch = 1)
          Right((pool4, _, _)) <- pool3.compound(sb2, epoch = 1)
        } yield (pool2, pool3, pool4)

        val (_, (pool2, pool3, pool4)) = action.run(RuntimeCtx.at(startAtHeight)).value

        val poolBox2 = pool2.toLedger[Ledger].setRegister(epochReg, 1)
        val poolBox3 = pool3.toLedger[Ledger].setRegister(epochReg, 1)
        val poolBox4 = pool4.toLedger[Ledger].setRegister(epochReg, 1)

        val (_, isValidFirstCompounding) =
          poolBox2.validator.run(RuntimeCtx(startAtHeight + epochStep, outputs = List(poolBox3))).value

        val (_, isValidSecondCompounding) =
          poolBox3.validator.run(RuntimeCtx(startAtHeight + epochStep, outputs = List(poolBox4))).value

        isValidFirstCompounding shouldBe true
        isValidSecondCompounding shouldBe true
      }

      it should s"validate illegal compound behaviour mirrored from simulation$testId" in {
        val startAtHeight            = epochLen * 2
        val action                   = pool01.deposit(input0)
        val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(programStart)).value
        val poolBox0                 = pool1.toLedger[Ledger]
        val action1                  = pool1.compound(sb1, epoch = 2)
        val (_, Left(pool2))         = action1.run(RuntimeCtx.at(startAtHeight)).value

        val reward = (pool01.conf.programBudget - 1L) / epochNum
        val X1     = pool01.reserves.X - reward
        val TMP1   = pool01.reserves.TMP + sb1.TMP

        val poolBox1 = new LqMiningPoolBox(
          boxId("LM_Pool_NFT_ID"),
          MinCollateralErg,
          DefaultCreationHeight,
          tokens = Coll(
            bytes("LM_Pool_NFT_ID") -> 1L,
            bytes("X")              -> X1,
            bytes("LQ")             -> pool01.reserves.LQ,
            bytes("vLQ")            -> pool01.reserves.vLQ,
            bytes("TMP")            -> TMP1
          ),
          registers = Map(
            4 -> Coll(),
            5 -> pool01.conf.programBudget,
            6 -> pool01.conf.maxRoundingError,
            7 -> pool01.execution.execBudget
          )
        )

        val (_, isValid) =
          poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1.setRegister(epochReg, 2)))).value

        pool2 shouldBe IllegalEpoch
        isValid shouldBe false

      }

      it should s"validate redeem behaviour before LM program start mirrored from simulation$testId" in {
        val startAtHeight             = programStart - 1
        val action                    = pool01.deposit(input0)
        val (_, Right((pool1, sb1)))  = action.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox1                  = pool1.toLedger[Ledger]
        val action1                   = pool1.redeem(sb1)
        val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox2                  = pool2.toLedger[Ledger]
        val (_, isValid)              = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
        out2.value shouldBe input0.value
        isValid shouldBe true
      }

      it should s"validate redeem behaviour during LM program mirrored from simulation$testId" in {
        val startAtHeight             = programStart - 1
        val action                    = pool01.deposit(input0)
        val (_, Right((pool1, sb1)))  = action.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox1                  = pool1.toLedger[Ledger]
        val action1                   = pool1.redeem(sb1)
        val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight + epochLen + 2)).value
        val poolBox2                  = pool2.toLedger[Ledger]
        val (_, isValid) =
          poolBox1.validator.run(RuntimeCtx(startAtHeight + epochLen + 2, outputs = List(poolBox2))).value
        out2.value shouldBe input0.value
        isValid shouldBe false
      }

      it should s"validate redeem behaviour after compounding mirrored from simulation$testId" in {
        val startAtHeight                    = programStart - 1
        val action                           = pool01.deposit(input0)
        val (_, Right((pool1, bundle11)))    = action.run(RuntimeCtx.at(startAtHeight)).value
        val action1                          = pool1.deposit(input1)
        val (_, Right((pool2, bundle21)))    = action1.run(RuntimeCtx.at(startAtHeight)).value
        val action3                          = pool2.compound(bundle11, epoch = 1)
        val (_, Right((pool3, bundle12, _))) = action3.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
        val action4                          = pool3.compound(bundle21, epoch = 1)
        val (_, Right((pool4, bundle22, _))) = action4.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
        val action5                          = pool4.redeem(bundle22)
        val (_, Right((pool5, out)))         = action5.run(RuntimeCtx.at(startAtHeight + epochLen + 2)).value
        val action6                          = pool5.redeem(bundle12)
        val (_, Right((pool6, _)))           = action6.run(RuntimeCtx.at(startAtHeight + epochLen + 2)).value
        val poolBox5                         = pool5.toLedger[Ledger]
        val poolBox6                         = pool6.toLedger[Ledger]
        val (_, isValid) =
          poolBox5.validator.run(RuntimeCtx(startAtHeight + epochLen + 2, outputs = List(poolBox6))).value
        out.value shouldBe input1.value
        isValid shouldBe true
      }

      it should s"validate illegal redeem behaviour (2) during compounding mirrored from simulation$testId" in {
        val startAtHeight                 = programStart - 1
        val action                        = pool01.deposit(input0)
        val (_, Right((pool1, bundle11))) = action.run(RuntimeCtx.at(startAtHeight)).value
        val action1                       = pool1.deposit(input1)
        val (_, Right((pool2, bundle21))) = action1.run(RuntimeCtx.at(startAtHeight)).value
        val action3                       = pool2.compound(bundle11, epoch = 1)
        val (_, Right((pool3, _, _)))     = action3.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
        val action4                       = pool3.redeem(bundle21)
        val (_, Right((pool4, _)))        = action4.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
        val poolBox3                      = pool3.toLedger[Ledger]
        val poolBox4                      = pool4.toLedger[Ledger]
        val (_, isValid) =
          poolBox3.validator.run(RuntimeCtx(startAtHeight + epochLen + 1, outputs = List(poolBox4))).value
        isValid shouldBe false
      }

      it should s"validate redeem behaviour after LM program end mirrored from simulation$testId" in {
        val startAtHeight             = programStart + 1
        val action                    = pool01.deposit(input0)
        val (_, Right((pool1, sb1)))  = action.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox1                  = pool1.toLedger[Ledger]
        val action1                   = pool1.redeem(sb1)
        val startAtHeight1            = programStart + epochNum * epochLen + pool01.conf.redeemLimitDelta
        val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight1)).value
        val poolBox2                  = pool2.toLedger[Ledger]
        val (_, isValid)              = poolBox1.validator.run(RuntimeCtx(startAtHeight1, outputs = List(poolBox2))).value
        out2.value shouldBe input0.value
        isValid shouldBe true
      }

      it should s"validate last redeem all behaviour mirrored from simulation$testId" in {
        val startAtHeight             = programStart + 1
        val action                    = pool01.deposit(input0)
        val (_, Right((pool1, sb1)))  = action.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox1                  = pool1.toLedger[Ledger]
        val action1                   = pool1.redeem(sb1)
        val startAtHeight1            = programStart + epochNum * epochLen + pool01.conf.redeemLimitDelta
        val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight1)).value
        val poolBox2                  = pool2.toLedger[Ledger]
        val (_, isValid)              = poolBox1.validator.run(RuntimeCtx(startAtHeight1, outputs = List(poolBox2))).value

        out2.value shouldBe input0.value
        isValid shouldBe true
      }

      it should s"validate increase execution budget behaviour mirrored from simulation$testId" in {
        val startAtHeight     = programStart + 1
        val action            = pool01.increaseExecutionBudget(1000)
        val (_, Right(pool1)) = action.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox1          = pool01.toLedger[Ledger]
        val poolBox2          = pool1.toLedger[Ledger]
        val (_, isValid)      = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
        isValid shouldBe true
      }

      it should s"prevent creation height decreasing$testId" in {
        val startAtHeight = programStart + 1
        val poolBox1      = pool01.toLedger[Ledger]
        val poolBox2 = new LqMiningPoolBox(
          boxId("LM_Pool_NFT_ID"),
          pool01.reserves.value + 1,
          0,
          tokens = Coll(
            bytes("LM_Pool_NFT_ID") -> 1L,
            bytes("X")              -> pool01.reserves.X,
            bytes("LQ")             -> pool01.reserves.LQ,
            bytes("vLQ")            -> pool01.reserves.vLQ,
            bytes("TMP")            -> pool01.reserves.TMP
          ),
          registers = Map(
            4 -> Coll(),
            5 -> pool01.conf.programBudget,
            6 -> pool01.conf.maxRoundingError,
            7 -> pool01.execution.execBudget
          )
        )
        val (_, isValid) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
        isValid shouldBe false
      }
    }
  }
}
