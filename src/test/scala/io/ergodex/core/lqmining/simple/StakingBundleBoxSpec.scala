package io.ergodex.core.lqmining.simple

import io.ergodex.core.Helpers.hex
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
      val input1: AssetInput[LQ] = AssetInput(lq * 2)
      val input2: AssetInput[LQ] = AssetInput(lq * 3)

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

        val (_, isValidPoolRedeemed) =
          poolBox2.validator.run(RuntimeCtx(compoundHeight, outputs = List(poolBox3))).value

        pool1.conf.epochAlloc - reward.value <= 2L shouldBe true

        isValidPool shouldBe true
        isValidCompoundReward shouldBe true

        isValidPoolRedeemed shouldBe true
        isValidCompoundRedeem shouldBe true
      }

      it should s"validate illegal compound behaviour mirrored from simulation$testId" in {
        val startAtHeight                = programStart
        val action                       = pool01.deposit(input0)
        val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                      = pool1.compound(bundle1, epoch = 1)
        val (_, Right((pool2, bundle2, reward))) =
          action1.run(RuntimeCtx.at(startAtHeight + pool01.conf.epochLen + 1)).value

        val poolBox1 = pool1.toLedger[Ledger].setRegister(epochReg, 1)
        val poolBox2 = pool2.toLedger[Ledger].setRegister(epochReg, 1)

        val (userBoxReward, _, bundleBox1, bundleBox2) =
          getCompoundTxBoxes(reward.value, bundle1.vLQ, bundle1.TMP, bundle2.TMP, validatorOutBytes = hex("bad_box"))

        val (_, isValidCompound) = bundleBox1.validator
          .run(
            RuntimeCtx(
              startAtHeight + pool01.conf.epochLen + 1,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward, bundleBox2)
            )
          )
          .value

        val (_, isValidPool) =
          poolBox1.validator.run(RuntimeCtx(startAtHeight + pool01.conf.epochLen + 1, outputs = List(poolBox2))).value

        isValidPool shouldBe true
        isValidCompound shouldBe false
      }

      it should s"validate late compound behaviour$testId" in {
        val startAtHeight                         = pool01.conf.programStart + epochLen * (epochNum + 1)
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
              inputs  = List(poolBox2.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox3.setRegister(epochReg, 2), userBoxReward2, bundleBox3)
            )
          )
          .value

        math.abs(reward2.value - reward3.value) <= 1 shouldBe true
        math.abs(reward3.value - pool01.conf.epochAlloc) <= 1 shouldBe true

        isValidPool2 shouldBe true
        isValidCompoundReward2 shouldBe true

        isValidPool3 shouldBe true
        isValidCompoundReward3 shouldBe true
      }

      it should s"validate two deposits before start compounding scenario$testId" in {
        val startAtHeight                          = programStart - 1
        val compoundHeight                         = startAtHeight + epochLen + 1
        val action                                 = pool01.deposit(input0)
        val (_, Right((pool1, bundle1)))           = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                                = pool1.deposit(input1)
        val (_, Right((pool2, bundle2)))           = action1.run(RuntimeCtx.at(programStart - 1)).value
        val action2                                = pool2.compound(bundle1, epoch = 1)
        val (_, Right((pool3, bundle12, reward1))) = action2.run(RuntimeCtx.at(compoundHeight)).value
        val action3                                = pool3.compound(bundle2, epoch = 1)
        val (_, Right((pool4, bundle22, reward2))) = action3.run(RuntimeCtx.at(compoundHeight)).value

        val poolBox2 = pool2.toLedger[Ledger]
        val poolBox3 = pool3.toLedger[Ledger]
        val poolBox4 = pool4.toLedger[Ledger]

        val (userBoxReward1, _, bundleBox1, bundleBox12) =
          getCompoundTxBoxes(reward1.value, bundle1.vLQ, bundle1.TMP, bundle12.TMP)

        val (userBoxReward2, _, bundleBox2, bundleBox22) =
          getCompoundTxBoxes(reward2.value, bundle2.vLQ, bundle2.TMP, bundle22.TMP)

        val (_, isValidCompoundReward1) = bundleBox1.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidPool1) = poolBox2.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidCompoundReward2) = bundleBox2.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 1), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 1), userBoxReward2, bundleBox22)
            )
          )
          .value

        val (_, isValidPool2) = poolBox3.validator
          .run(
            RuntimeCtx(
              compoundHeight,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 1), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 1), userBoxReward2, bundleBox22)
            )
          )
          .value

        math.abs(reward1.value + reward2.value - pool01.conf.epochAlloc) <= 1 shouldBe true

        isValidPool1 shouldBe true
        isValidCompoundReward1 shouldBe true

        isValidPool2 shouldBe true
        isValidCompoundReward2 shouldBe true
      }

      it should s"validate compounding after first epoch with additional deposit$testId" in {
        val startAtHeight   = programStart - 1
        val compound1Height = startAtHeight + 2 * epochLen + 1
        val compound2Height = startAtHeight + 3 * epochLen + 1

        val action                       = pool01.deposit(input0)
        val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                      = pool1.deposit(input1)
        val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(programStart + 1)).value

        val action2                                = pool2.compound(bundle1, epoch = 1)
        val (_, Right((pool3, bundle12, reward1))) = action2.run(RuntimeCtx.at(compound1Height)).value

        val action3                                 = pool3.compound(bundle12, epoch = 2)
        val (_, Right((pool4, bundle13, reward12))) = action3.run(RuntimeCtx.at(compound2Height)).value

        val action4                                 = pool4.compound(bundle2, epoch = 2)
        val (_, Right((pool5, bundle22, reward22))) = action4.run(RuntimeCtx.at(compound2Height)).value

        val poolBox2 = pool2.toLedger[Ledger]
        val poolBox3 = pool3.toLedger[Ledger]
        val poolBox4 = pool4.toLedger[Ledger]
        val poolBox5 = pool5.toLedger[Ledger]

        val (userBoxReward1, _, bundleBox1, bundleBox12) =
          getCompoundTxBoxes(reward1.value, bundle1.vLQ, bundle1.TMP, bundle12.TMP)

        val (userBoxReward2, _, bundleBox2, bundleBox13) =
          getCompoundTxBoxes(reward12.value, bundle12.vLQ, bundle12.TMP, bundle13.TMP)

        val (userBoxReward3, _, bundleBox3, bundleBox22) =
          getCompoundTxBoxes(reward22.value, bundle2.vLQ, bundle2.TMP, bundle22.TMP)

        val (_, isValidCompoundReward1) = bundleBox1.validator
          .run(
            RuntimeCtx(
              compound1Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidPool1) = poolBox2.validator
          .run(
            RuntimeCtx(
              compound1Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidCompoundReward2) = bundleBox2.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 2), userBoxReward2, bundleBox13)
            )
          )
          .value

        val (_, isValidPool2) = poolBox3.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 2), userBoxReward2, bundleBox13)
            )
          )
          .value

        val (_, isValidCompoundReward3) = bundleBox3.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox4.setRegister(epochReg, 2), bundleBox3),
              outputs = List(poolBox5.setRegister(epochReg, 2), userBoxReward3, bundleBox22)
            )
          )
          .value

        val (_, isValidPool3) = poolBox4.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox4.setRegister(epochReg, 2), bundleBox3),
              outputs = List(poolBox5.setRegister(epochReg, 2), userBoxReward3, bundleBox22)
            )
          )
          .value

        val totalRewards = reward1.value + reward12.value + reward22.value

        math.abs(reward1.value - pool01.conf.epochAlloc) <= 2L shouldBe true
        programBudget - totalRewards <= pool01.conf.epochAlloc + 2L shouldBe true
        math.abs(reward12.value + reward22.value - pool01.conf.epochAlloc) <= 2L shouldBe true

        isValidPool1 shouldBe true
        isValidCompoundReward1 shouldBe true

        isValidPool2 shouldBe true
        isValidCompoundReward2 shouldBe true

        isValidPool3 shouldBe true
        isValidCompoundReward3 shouldBe true

      }

      it should s"validate compounding after first epoch with additional illegal deposit during compounding$testId" in {
        val startAtHeight   = programStart - 1
        val compound1Height = startAtHeight + epochLen + 1
        val compound2Height = startAtHeight + 2 * epochLen + 1

        val action                       = pool01.deposit(input0)
        val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                      = pool1.deposit(input1)
        val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(programStart + 1)).value

        val action2                                = pool2.compound(bundle1, epoch = 1)
        val (_, Right((pool3, bundle12, reward1))) = action2.run(RuntimeCtx.at(compound1Height)).value

        val action3                                 = pool3.compound(bundle12, epoch = 2)
        val (_, Right((pool4, bundle13, reward12))) = action3.run(RuntimeCtx.at(compound2Height)).value

        val action4                = pool4.deposit(input2)
        val (_, Right((pool5, _))) = action4.run(RuntimeCtx.at(compound2Height)).value

        val action5                                 = pool5.compound(bundle2, epoch = 2)
        val (_, Right((pool6, bundle22, reward22))) = action5.run(RuntimeCtx.at(compound2Height + 1)).value

        val poolBox2 = pool2.toLedger[Ledger]
        val poolBox3 = pool3.toLedger[Ledger]
        val poolBox4 = pool4.toLedger[Ledger]
        val poolBox5 = pool5.toLedger[Ledger]
        val poolBox6 = pool6.toLedger[Ledger]

        val (userBoxReward1, _, bundleBox1, bundleBox12) =
          getCompoundTxBoxes(reward1.value, bundle1.vLQ, bundle1.TMP, bundle12.TMP)

        val (userBoxReward2, _, bundleBox2, bundleBox13) =
          getCompoundTxBoxes(reward12.value, bundle12.vLQ, bundle12.TMP, bundle13.TMP)

        val (userBoxReward3, _, bundleBox3, bundleBox22) =
          getCompoundTxBoxes(reward22.value, bundle2.vLQ, bundle2.TMP, bundle22.TMP)

        val (_, isValidCompoundReward1) = bundleBox1.validator
          .run(
            RuntimeCtx(
              compound1Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidPool1) = poolBox2.validator
          .run(
            RuntimeCtx(
              compound1Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidCompoundReward2) = bundleBox2.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 2), userBoxReward2, bundleBox13)
            )
          )
          .value

        val (_, isValidPool2) = poolBox3.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 2), userBoxReward2, bundleBox13)
            )
          )
          .value

        val (_, isValidCompoundReward3) = bundleBox3.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox5.setRegister(epochReg, 2), bundleBox3),
              outputs = List(poolBox6.setRegister(epochReg, 2), userBoxReward3, bundleBox22)
            )
          )
          .value

        val (_, isValidPool3) = poolBox5.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox5.setRegister(epochReg, 2), bundleBox3),
              outputs = List(poolBox6.setRegister(epochReg, 2), userBoxReward3, bundleBox22)
            )
          )
          .value

        isValidPool1 shouldBe true
        isValidCompoundReward1 shouldBe true

        isValidPool2 shouldBe true
        isValidCompoundReward2 shouldBe true

        isValidPool3 shouldBe false
        isValidCompoundReward3 shouldBe true

      }

      it should s"validate compounding and redeem after program$testId" in {
        val startAtHeight   = programStart - 1
        val compound1Height = startAtHeight + epochLen + 1
        val compound2Height = startAtHeight + 2 * epochLen + 1
        val compound3Height = startAtHeight + 4 * epochLen + 1

        val action                       = pool01.deposit(input0)
        val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
        val action1                      = pool1.deposit(input1)
        val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(programStart + 1)).value

        val action2                                 = pool2.compound(bundle1, epoch = 1)
        val (_, Right((pool3, bundle12, reward1)))  = action2.run(RuntimeCtx.at(compound1Height)).value
        val action3                                 = pool3.compound(bundle12, epoch = 2)
        val (_, Right((pool4, bundle13, reward12))) = action3.run(RuntimeCtx.at(compound2Height)).value

        val action4                      = pool4.deposit(input2)
        val (_, Right((pool5, bundle3))) = action4.run(RuntimeCtx.at(compound2Height - 2)).value

        val action5                                 = pool5.compound(bundle2, epoch = 2)
        val (_, Right((pool6, bundle22, reward22))) = action5.run(RuntimeCtx.at(compound2Height)).value

        val action6                                   = pool6.compound(bundle13, epoch = 3)
        val (_, Right((pool7, bundle133, reward133))) = action6.run(RuntimeCtx.at(compound3Height)).value

        val action7                                  = pool7.compound(bundle22, epoch = 3)
        val (_, Right((pool8, bundle23, reward233))) = action7.run(RuntimeCtx.at(compound3Height)).value
        val action8                                  = pool8.compound(bundle3, epoch = 3)
        val (_, Right((pool9, bundle33, reward333))) = action8.run(RuntimeCtx.at(compound3Height)).value

        val action9                     = pool9.redeem(bundle33)
        val (_, Right((pool10, out33))) = action9.run(RuntimeCtx.at(compound3Height)).value

        val poolBox2 = pool2.toLedger[Ledger]
        val poolBox3 = pool3.toLedger[Ledger]
        val poolBox4 = pool4.toLedger[Ledger]
        val poolBox5 = pool5.toLedger[Ledger]
        val poolBox6 = pool6.toLedger[Ledger]
        val poolBox8 = pool8.toLedger[Ledger]
        val poolBox9 = pool9.toLedger[Ledger]
        val poolBox10 = pool10.toLedger[Ledger]

        val (userBoxReward1, _, bundleBox1, bundleBox12) =
          getCompoundTxBoxes(reward1.value, bundle1.vLQ, bundle1.TMP, bundle12.TMP)

        val (userBoxReward2, _, bundleBox2, bundleBox13) =
          getCompoundTxBoxes(reward12.value, bundle12.vLQ, bundle12.TMP, bundle13.TMP)

        val (userBoxReward3, _, bundleBox3, bundleBox22) =
          getCompoundTxBoxes(reward22.value, bundle2.vLQ, bundle2.TMP, bundle22.TMP)

        val (userBoxReward333, _, bundleBox33, bundleBox333) =
          getCompoundTxBoxes(reward333.value, bundle3.vLQ, bundle3.TMP, bundle33.TMP)

        val (userBoxRedeemed, redeemBox) = getRedeemTxBoxes(bundle33.vLQ, out33.value)

        val (_, isValidCompoundReward1) = bundleBox1.validator
          .run(
            RuntimeCtx(
              compound1Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidPool1) = poolBox2.validator
          .run(
            RuntimeCtx(
              compound1Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox2.setRegister(epochReg, 1), bundleBox1),
              outputs = List(poolBox3.setRegister(epochReg, 1), userBoxReward1, bundleBox12)
            )
          )
          .value

        val (_, isValidCompoundReward2) = bundleBox2.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 2), userBoxReward2, bundleBox13)
            )
          )
          .value

        val (_, isValidPool2) = poolBox3.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox3.setRegister(epochReg, 2), bundleBox2),
              outputs = List(poolBox4.setRegister(epochReg, 2), userBoxReward2, bundleBox13)
            )
          )
          .value

        val (_, isValidCompoundReward3) = bundleBox3.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox5.setRegister(epochReg, 2), bundleBox3),
              outputs = List(poolBox6.setRegister(epochReg, 2), userBoxReward3, bundleBox22)
            )
          )
          .value

        val (_, isValidPool3) = poolBox5.validator
          .run(
            RuntimeCtx(
              compound2Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox5.setRegister(epochReg, 2), bundleBox3),
              outputs = List(poolBox6.setRegister(epochReg, 2), userBoxReward3, bundleBox22)
            )
          )
          .value

        val (_, isValidCompoundReward33) = bundleBox33.validator
          .run(
            RuntimeCtx(
              compound3Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox8.setRegister(epochReg, 3), bundleBox33),
              outputs = List(poolBox9.setRegister(epochReg, 3), userBoxReward333, bundleBox333)
            )
          )
          .value

        val (_, isValidPool33) = poolBox8.validator
          .run(
            RuntimeCtx(
              compound3Height,
              vars    = Map(0 -> 1, 1 -> 2),
              inputs  = List(poolBox8.setRegister(epochReg, 3), bundleBox33),
              outputs = List(poolBox9.setRegister(epochReg, 3), userBoxReward333, bundleBox333)
            )
          )
          .value

        val (_, isValidPoolBatch) = poolBox6.validator
          .run(
            RuntimeCtx(
              compound3Height,
              inputs  = List(poolBox6.setRegister(epochReg, 3)),
              outputs = List(poolBox9.setRegister(epochReg, 3))
            )
          )
          .value

        val (_, isValidCompoundRedeem) = bundleBox333.validator
          .run(
            RuntimeCtx(
              compound3Height,
              inputs  = List(poolBox9, bundleBox333, redeemBox),
              outputs = List(poolBox10, userBoxRedeemed)
            )
          )
          .value

        val (_, isValidPoolRedeemed) =
          poolBox9.validator.run(RuntimeCtx(compound3Height, outputs = List(poolBox10))).value

        val totalRewards =
          reward1.value + reward12.value + reward22.value + reward133.value + reward233.value + reward333.value

        programBudget - totalRewards <= 2L shouldBe true
        math.abs(reward1.value - pool01.conf.epochAlloc) <= 2L shouldBe true
        math.abs(reward12.value + reward22.value - pool01.conf.epochAlloc) <= 2L shouldBe true
        math.abs(reward133.value + reward233.value + reward333.value - pool01.conf.epochAlloc) <= 2L shouldBe true

        isValidPool1 shouldBe true
        isValidCompoundReward1 shouldBe true

        isValidPool2 shouldBe true
        isValidCompoundReward2 shouldBe true

        isValidPool3 shouldBe true
        isValidCompoundReward3 shouldBe true

        isValidPool33 shouldBe true
        isValidCompoundReward33 shouldBe true

        isValidPoolBatch shouldBe true

        isValidPoolRedeemed shouldBe true
        isValidCompoundRedeem shouldBe true

      }
    }
  }
}
