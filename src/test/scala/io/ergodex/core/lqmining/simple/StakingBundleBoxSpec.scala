package io.ergodex.core.lqmining.simple

import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.simple.LMPool._
import io.ergodex.core.lqmining.simple.Token._
import io.ergodex.core.lqmining.simple.TxBoxes._
import io.ergodex.core.lqmining.simple.generators.{DepositGen, lmConfGen}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class StakingBundleBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  forAll(lmConfGen) { conf =>
    forAll(DepositGen(1)) { case lq =>
      val epochLen         = conf.epochLen
      val epochNum         = conf.epochNum
      val programStart     = conf.programStart
      val redeemLimitDelta = conf.redeemLimitDelta
      val programBudget    = conf.programBudget
      val maxRoundingError = conf.maxRoundingError
      val epochReg         = 8

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

      val input0: AssetInput[LQ] = AssetInput(lq)

      it should s"validate compound and redeem behaviour$testId" in {
        val startAtHeight                        = programStart - 1
        val compoundHeight                       = programStart + epochLen + 1
        val action                               = pool01.deposit(input0)
        val (_, Right((pool1, bundle1)))         = action.run(RuntimeCtx.at(startAtHeight)).value
        val action1                              = pool1.compound(bundle1, epoch = 1)
        val (_, Right((pool2, bundle2, reward))) = action1.run(RuntimeCtx.at(compoundHeight)).value
        val action2                              = pool2.redeem(bundle2)
        val (_, Right((pool3, out3)))            = action2.run(RuntimeCtx.at(compoundHeight)).value

        val poolBox1 = pool1.toLedger[Ledger]
        val poolBox2 = pool2.toLedger[Ledger]
        val poolBox3 = pool3.toLedger[Ledger]

        val (userBoxReward, _, bundleBox1, bundleBox2) =
          getCompoundTxBoxes(reward.value, bundle1.vLQ, bundle1.TMP, bundle2.TMP)

        val (userBoxRedeemed, redeemBox) = getRedeemTxBoxes(bundle2.vLQ, out3.value)

        val (_, isValidCompoundReward) = bundleBox1.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward, bundleBox2)
            )
          )
          .value

        val (_, isValidPool) = poolBox1.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward, bundleBox2)
            )
          )
          .value

        val (_, isValidCompoundRedeem) = bundleBox2.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              inputs  = List(poolBox2, bundleBox2, redeemBox),
              outputs = List(poolBox3, userBoxRedeemed)
            )
          )
          .value

        val (_, isValidPoolRedeemed) = poolBox2.validator.run(RuntimeCtx(compoundHeight, outputs = List(poolBox3))).value

        isValidPool shouldBe true
        isValidCompoundReward shouldBe true

        isValidPoolRedeemed shouldBe true
        isValidCompoundRedeem shouldBe true
      }

      it should s"validate illegal compound and redeem behaviour mirrored from simulation$testId" in {
        val startAtHeight                = programStart
        val action                       = pool01.deposit(input0)
        val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                      = pool1.compound(bundle1, epoch = 1)
        val (_, Right((pool2, bundle2, reward))) =
          action1.run(RuntimeCtx.at(startAtHeight + pool01.conf.epochLen * 3)).value
        val action2                = pool2.redeem(bundle2)
        val (_, Right((pool3, _))) = action2.run(RuntimeCtx.at(startAtHeight)).value

        val poolBox1 = pool1.toLedger[Ledger]
        val poolBox2 = pool2.toLedger[Ledger]
        val poolBox3 = pool3.toLedger[Ledger]

        val (userBoxReward, _, bundleBox1, bundleBox2) =
          getCompoundTxBoxes(reward.value, bundle1.vLQ, bundle1.TMP, bundle2.TMP, validatorOutBytes = "bad_box")

        val (_, isValidCompound) = bundleBox1.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward, bundleBox2)
            )
          )
          .value

        val (_, isValidPool) = poolBox2.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox3))).value

        isValidPool shouldBe true
        isValidCompound shouldBe false
      }

      it should s"validate late compound behaviour$testId" in {
        val startAtHeight                         = pool01.conf.programStart + epochLen * (epochNum - 1)
        val action                                = pool01.deposit(input0)
        val (_, Right((pool1, bundle1)))          = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                               = pool1.compound(bundle1, epoch = 1)
        val (_, Right((pool2, bundle2, reward2))) = action1.run(RuntimeCtx.at(startAtHeight)).value
        val action2                               = pool2.compound(bundle2, epoch = 2)
        val (_, Right((pool3, bundle3, reward3))) = action2.run(RuntimeCtx.at(startAtHeight)).value
        val poolBox1                              = pool1.toLedger[Ledger]
        val poolBox2                              = pool2.toLedger[Ledger]
        val poolBox3                              = pool3.toLedger[Ledger]

        val (userBoxReward1, _, bundleBox1, bundleBox2) =
          getCompoundTxBoxes(reward2.value, bundle1.vLQ, bundle1.TMP, bundle2.TMP)

        val (userBoxReward2, _, _, bundleBox3) =
          getCompoundTxBoxes(reward3.value, bundle2.vLQ, bundle2.TMP, bundle3.TMP)

        val (_, isValidCompoundReward2) = bundleBox1.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward1, bundleBox2)
            )
          )
          .value

        val (_, isValidPool2) = poolBox1.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward1, bundleBox2)
            )
          )
          .value

        val (_, isValidCompoundReward3) = bundleBox2.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox3.setRegister(epochReg, 2), userBoxReward2, bundleBox3)
            )
          )
          .value

        val (_, isValidPool3) = poolBox2.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox2),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward2, bundleBox3)
            )
          )
          .value

        isValidPool2 shouldBe true
        isValidCompoundReward2 shouldBe true

        isValidPool3 shouldBe true
        isValidCompoundReward3 shouldBe true
      }
    }
  }
}
