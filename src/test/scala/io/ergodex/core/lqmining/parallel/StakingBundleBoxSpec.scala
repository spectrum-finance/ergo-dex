package io.ergodex.core.lqmining.parallel

import io.ergodex.core.Helpers.hex
import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.parallel.Token._
import io.ergodex.core.lqmining.parallel.TxBoxes._
import io.ergodex.core.lqmining.parallel.generators._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class StakingBundleBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  forAll(lmConfGen) { conf =>
    forAll(DepositGen) { lq =>
      forAll(MultGen) { mults =>
        val epochLen         = conf.epochLen
        val epochNum         = conf.epochNum
        val programStart     = conf.programStart
        val redeemLimitDelta = conf.redeemLimitDelta
        val mainBudget       = conf.mainBudget
        val optBudget        = conf.optBudget
        val maxRoundingError = conf.maxRoundingError

        val testId = Random.nextLong()

        val pool01: LMPool[Ledger] =
          LMPool.init(
            epochLen,
            epochNum,
            mainBudget,
            optBudget,
            programStart,
            redeemLimitDelta,
            maxRoundingError
          )

        val input0: AssetInput[LQ] = AssetInput(mults._1 * lq)
        val input1: AssetInput[LQ] = AssetInput(mults._2 * lq)
        val input2: AssetInput[LQ] = AssetInput(mults._3 * lq)

        it should s"validate compound and redeem behaviour$testId" in {
          val startAtHeight                = programStart - 1
          val compoundHeight               = programStart + epochLen + 1
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value
          val action1                      = pool1.compound(bundle1, epoch = 1)
          val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(compoundHeight)).value
          val action2                      = pool2.redeem(bundle2)
          val (_, Right((pool3, out3)))    = action2.run(RuntimeCtx.at(compoundHeight)).value

          val poolBox1 = pool1.toLedger[Ledger]
          val poolBox2 = pool2.toLedger[Ledger]
          val poolBox3 = pool3.toLedger[Ledger]

          val (bundleBox1, bundleBox2) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP
            )

          val (userBoxRedeemed, redeemBox, bundleBox) =
            getRedeemTxBoxes(bundle2.TMP, bundle2.rewardMain, bundle2.rewardOpt, bundle2.vLQ, out3.value)

          val (_, isValidCompoundReward) = bundleBox1.validator
            .run(
              RuntimeCtx(
                compoundHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox1, bundleBox1),
                outputs = List(poolBox2, bundleBox2)
              )
            )
            .value

          val (_, isValidPool) = poolBox1.validator
            .run(
              RuntimeCtx(
                compoundHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox1, bundleBox1),
                outputs = List(poolBox2, bundleBox2)
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

          isValidPool shouldBe true
          isValidCompoundReward shouldBe true

          isValidPoolRedeemed shouldBe false
          isValidCompoundRedeem shouldBe false
        }

        it should s"validate illegal compound behaviour mirrored from simulation$testId" in {
          val startAtHeight                = programStart
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
          val action1                      = pool1.compound(bundle1, epoch = 1)
          val (_, Right((pool2, bundle2))) =
            action1.run(RuntimeCtx.at(startAtHeight + pool01.conf.epochLen + 1)).value

          val poolBox1 = pool1.toLedger[Ledger]
          val poolBox2 = pool2.toLedger[Ledger]

          val (bundleBox1, bundleBox2) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP,
              validatorOutBytes = hex("bad_box")
            )

          val (_, isValidCompound) = bundleBox1.validator
            .run(
              RuntimeCtx(
                startAtHeight + pool01.conf.epochLen + 1,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox1, bundleBox1),
                outputs = List(poolBox2, bundleBox2)
              )
            )
            .value

          val (_, isValidPool) =
            poolBox1.validator.run(RuntimeCtx(startAtHeight + pool01.conf.epochLen + 1, outputs = List(poolBox2))).value

          isValidPool shouldBe true
          isValidCompound shouldBe false
        }

        it should s"validate late compound behaviour$testId" in {
          val startAtHeight                = pool01.conf.programStart + epochLen * (epochNum + 1)
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
          val action1                      = pool1.compound(bundle1, epoch = 1)
          val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(startAtHeight)).value
          val action2                      = pool2.updateBudgets(compounded_epoch = 1)
          val (_, Right((pool3)))          = action2.run(RuntimeCtx.at(startAtHeight)).value
          val action3                      = pool3.compound(bundle2, epoch = 2)
          val (_, Right((pool4, bundle3))) = action3.run(RuntimeCtx.at(startAtHeight)).value
          val poolBox1                     = pool1.toLedger[Ledger]
          val poolBox2                     = pool2.toLedger[Ledger]
          val poolBox3                     = pool3.toLedger[Ledger]
          val poolBox4                     = pool4.toLedger[Ledger]

          val (bundleBox1, bundleBox2) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP
            )

          val (_, bundleBox3) =
            getCompoundTxBoxes(
              bundle2.vLQ,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP,
              bundle3.rewardMain,
              bundle3.rewardOpt,
              bundle3.TMP
            )

          val (_, isValidPool1) = poolBox1.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox1, bundleBox1),
                outputs = List(poolBox2, bundleBox2)
              )
            )
            .value
          val (_, isValidCompoundReward1) = bundleBox1.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox1, bundleBox1),
                outputs = List(poolBox2, bundleBox2)
              )
            )
            .value

          val (_, isValidPool2) = poolBox2.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                inputs  = List(poolBox2),
                outputs = List(poolBox3)
              )
            )
            .value

          val (_, isValidPool3) = poolBox3.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                vars    = Map(0 -> 1, 0 -> 1),
                inputs  = List(poolBox3, bundleBox2),
                outputs = List(poolBox4, bundleBox3)
              )
            )
            .value

          val (_, isValidCompoundReward2) = bundleBox2.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox2),
                outputs = List(poolBox4, bundleBox3)
              )
            )
            .value

          math.abs(bundle2.rewardMain - pool01.conf.mainBudget / epochNum) <= 2L shouldBe true
          math.abs(bundle2.rewardOpt - pool01.conf.optBudget / epochNum) <= 2L shouldBe true

          math.abs(bundle3.rewardMain - pool01.conf.mainBudget / epochNum * 2) <= 2L shouldBe true
          math.abs(bundle3.rewardOpt - pool01.conf.optBudget / epochNum * 2) <= 2L shouldBe true

          isValidPool1 shouldBe true
          isValidCompoundReward1 shouldBe true

//          isValidPool2 shouldBe true

          isValidPool3 shouldBe true
          isValidCompoundReward2 shouldBe true
        }

        it should s"validate two deposits before start compounding scenario$testId" in {
          val startAtHeight                 = programStart - 1
          val compoundHeight                = startAtHeight + epochLen + 1
          val action                        = pool01.deposit(input0)
          val (_, Right((pool1, bundle1)))  = action.run(RuntimeCtx.at(programStart - 1)).value
          val action1                       = pool1.deposit(input1)
          val (_, Right((pool2, bundle2)))  = action1.run(RuntimeCtx.at(programStart - 1)).value
          val action2                       = pool2.compound(bundle1, epoch = 1)
          val (_, Right((pool3, bundle12))) = action2.run(RuntimeCtx.at(compoundHeight)).value
          val action3                       = pool3.compound(bundle2, epoch = 1)
          val (_, Right((pool4, bundle22))) = action3.run(RuntimeCtx.at(compoundHeight)).value

          val poolBox2 = pool2.toLedger[Ledger]
          val poolBox3 = pool3.toLedger[Ledger]
          val poolBox4 = pool4.toLedger[Ledger]

          val (bundleBox1, bundleBox12) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle12.TMP
            )

          val (bundleBox2, bundleBox22) =
            getCompoundTxBoxes(
              bundle2.vLQ,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP,
              bundle22.rewardMain,
              bundle22.rewardOpt,
              bundle22.TMP
            )

          val (_, isValidCompoundReward1) = bundleBox1.validator
            .run(
              RuntimeCtx(
                compoundHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3, bundleBox12)
              )
            )
            .value

          val (_, isValidPool1) = poolBox2.validator
            .run(
              RuntimeCtx(
                compoundHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3, bundleBox12)
              )
            )
            .value

          val (_, isValidCompoundReward2) = bundleBox2.validator
            .run(
              RuntimeCtx(
                compoundHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox2),
                outputs = List(poolBox4, bundleBox22)
              )
            )
            .value

          val (_, isValidPool2) = poolBox3.validator
            .run(
              RuntimeCtx(
                compoundHeight,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox2),
                outputs = List(poolBox4, bundleBox22)
              )
            )
            .value

          math.abs(bundle22.rewardMain + bundle12.rewardMain - pool01.conf.mainBudget / epochNum) <= 2L shouldBe true
          math.abs(bundle22.rewardOpt + bundle12.rewardOpt - pool01.conf.optBudget / epochNum) <= 2L shouldBe true

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

          val action2                        = pool2.compound(bundle1, epoch = 1)
          val (_, Right((pool3_, bundle12))) = action2.run(RuntimeCtx.at(compound1Height)).value

          val action2_            = pool3_.updateBudgets(compounded_epoch = 1)
          val (_, Right((pool3))) = action2_.run(RuntimeCtx.at(compound2Height)).value

          val action3                       = pool3.compound(bundle12, epoch = 2)
          val (_, Right((pool4, bundle13))) = action3.run(RuntimeCtx.at(compound2Height)).value

          val action4                       = pool4.compound(bundle2, epoch = 2)
          val (_, Right((pool5, bundle22))) = action4.run(RuntimeCtx.at(compound2Height)).value

          val poolBox3_ = pool3_.toLedger[Ledger]
          val poolBox2  = pool2.toLedger[Ledger]
          val poolBox3  = pool3.toLedger[Ledger]
          val poolBox4  = pool4.toLedger[Ledger]
          val poolBox5  = pool5.toLedger[Ledger]

          val (bundleBox1, bundleBox12) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle12.TMP
            )

          val (_, bundleBox13) =
            getCompoundTxBoxes(
              bundle12.vLQ,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle12.TMP,
              bundle13.rewardMain,
              bundle13.rewardOpt,
              bundle13.TMP
            )

          val (bundleBox2, bundleBox22) =
            getCompoundTxBoxes(
              bundle2.vLQ,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP,
              bundle22.rewardMain,
              bundle22.rewardOpt,
              bundle22.TMP
            )

          val (_, isValidCompoundReward1) = bundleBox1.validator
            .run(
              RuntimeCtx(
                compound1Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3_, bundleBox12)
              )
            )
            .value

          val (_, isValidPool1) = poolBox2.validator
            .run(
              RuntimeCtx(
                compound1Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3_, bundleBox12)
              )
            )
            .value

          val (_, isValidPool3_) = poolBox3_.validator
            .run(
              RuntimeCtx(
                compound1Height,
                inputs  = List(poolBox3_),
                outputs = List(poolBox3)
              )
            )
            .value

          val (_, isValidCompoundReward2) = bundleBox12.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox12),
                outputs = List(poolBox4, bundleBox13)
              )
            )
            .value

          val (_, isValidPool2) = poolBox3.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox12),
                outputs = List(poolBox4, bundleBox13)
              )
            )
            .value

          val (_, isValidCompoundReward3) = bundleBox2.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox4, bundleBox2),
                outputs = List(poolBox5, bundleBox22)
              )
            )
            .value

          val (_, isValidPool3) = poolBox4.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox4, bundleBox2),
                outputs = List(poolBox5, bundleBox22)
              )
            )
            .value

          val totalMainRewards = bundle13.rewardMain + bundle22.rewardMain
          val totalOptRewards  = bundle13.rewardOpt + bundle22.rewardOpt

          math.abs(bundle12.rewardMain - pool01.budgets.mainBudget / epochNum) <= 2L shouldBe true
          math.abs(bundle12.rewardOpt - pool01.budgets.optBudget / epochNum) <= 2L shouldBe true

          pool01.budgets.mainBudget - totalMainRewards <= pool01.budgets.mainBudget / epochNum + 2L shouldBe true
          pool01.budgets.optBudget - totalOptRewards <= pool01.budgets.optBudget / epochNum + 2L shouldBe true

          isValidPool1 shouldBe true
          isValidCompoundReward1 shouldBe true

//          isValidPool3_ shouldBe true

          isValidPool2 shouldBe true
          isValidCompoundReward2 shouldBe true

          isValidPool3 shouldBe true
          isValidCompoundReward3 shouldBe true

        }

        it should s"validate compounding after first epoch with additional illegal deposit during compounding$testId" in {
          val startAtHeight   = programStart - 1
          val compound1Height = startAtHeight + epochLen + 1
          val compound2Height = startAtHeight + 2 * epochLen + 2

          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
          val action1                      = pool1.deposit(input1)
          val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(programStart + 1)).value

          val action2                        = pool2.compound(bundle1, epoch = 1)
          val (_, Right((pool3_, bundle12))) = action2.run(RuntimeCtx.at(compound1Height)).value

          val action2_            = pool3_.updateBudgets(compounded_epoch = 1)
          val (_, Right((pool3))) = action2_.run(RuntimeCtx.at(compound1Height)).value

          val action3                       = pool3.compound(bundle12, epoch = 2)
          val (_, Right((pool4, bundle13))) = action3.run(RuntimeCtx.at(compound2Height)).value

          val action4                = pool4.deposit(input2)
          val (_, Right((pool5, _))) = action4.run(RuntimeCtx.at(compound2Height)).value

          val action5                       = pool5.compound(bundle2, epoch = 2)
          val (_, Right((pool6, bundle22))) = action5.run(RuntimeCtx.at(compound2Height + 1)).value

          val poolBox2  = pool2.toLedger[Ledger]
          val poolBox3  = pool3.toLedger[Ledger]
          val poolBox3_ = pool3_.toLedger[Ledger]
          val poolBox4  = pool4.toLedger[Ledger]
          val poolBox5  = pool5.toLedger[Ledger]
          val poolBox6  = pool6.toLedger[Ledger]

          val (bundleBox1, bundleBox12) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle12.TMP
            )

          val (bundleBox2, bundleBox13) =
            getCompoundTxBoxes(
              bundle12.vLQ,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle12.TMP,
              bundle13.rewardMain,
              bundle13.rewardOpt,
              bundle13.TMP
            )

          val (bundleBox3, bundleBox22) =
            getCompoundTxBoxes(
              bundle2.vLQ,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle2.TMP,
              bundle22.rewardMain,
              bundle22.rewardOpt,
              bundle22.TMP
            )

          val (_, isValidCompoundReward1) = bundleBox1.validator
            .run(
              RuntimeCtx(
                compound1Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3_, bundleBox12)
              )
            )
            .value

          val (_, isValidPool1) = poolBox2.validator
            .run(
              RuntimeCtx(
                compound1Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3_, bundleBox12)
              )
            )
            .value

          val (_, isValidCompoundReward2) = bundleBox2.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox2),
                outputs = List(poolBox4, bundleBox13)
              )
            )
            .value

          val (_, isValidPool2) = poolBox3.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox3, bundleBox2),
                outputs = List(poolBox4, bundleBox13)
              )
            )
            .value

          val (_, isValidPool3) = poolBox5.validator
            .run(
              RuntimeCtx(
                compound2Height,
                inputs  = List(poolBox4, bundleBox3),
                outputs = List(poolBox5, bundleBox22)
              )
            )
            .value

          isValidPool1 shouldBe true
          isValidCompoundReward1 shouldBe true

          isValidPool2 shouldBe true
          isValidCompoundReward2 shouldBe true

          isValidPool3 shouldBe false

        }

      }
    }
  }
}
