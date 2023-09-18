package io.ergodex.core.lqmining.parallel

import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.parallel.Token.LQ
import io.ergodex.core.lqmining.parallel.TxBoxes._
import io.ergodex.core.lqmining.parallel.generators.{lmConfGen, DepositGen, MultGen}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class FullProgramSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
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
        val mainBudgetAdd          = mainBudget * mults._3
        val optBudgetAdd           = mainBudget * mults._1

        it should s"validate full program scenario$testId" in {
          val startAtHeight   = programStart - 1
          val compound1Height = startAtHeight + epochLen + 2
          val compound2Height = startAtHeight + 2 * epochLen + 2
          val compound3Height = startAtHeight + 3 * epochLen + 2

          // Before start (1 deposit):
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value

          // First epoch (1 deposit):
          val action1                      = pool1.deposit(input1)
          val (_, Right((pool2, bundle2))) = action1.run(RuntimeCtx.at(programStart + 1)).value

          val action2                        = pool2.compound(bundle1, epoch = 1)
          val (_, Right((pool3_, bundle11))) = action2.run(RuntimeCtx.at(compound1Height)).value

          val action2_            = pool3_.updateBudgets(compounded_epoch = 1)
          val (_, Right((pool3))) = action2_.run(RuntimeCtx.at(compound1Height)).value

          // Second epoch (1 main budget deposit):
          val action3              = pool3.depositBudget(optBudgetAdd, 1)
          val (_, Right((pool33))) = action3.run(RuntimeCtx.at(compound1Height + 1)).value

          val action33 = pool33.depositBudget(mainBudgetAdd, 0)

          val (_, Right((pool4))) = action33.run(RuntimeCtx.at(compound1Height + 1)).value

          val action4                       = pool4.compound(bundle11, epoch = 2)
          val (_, Right((pool5, bundle12))) = action4.run(RuntimeCtx.at(compound2Height)).value

          val action5                        = pool5.compound(bundle2, epoch = 2)
          val (_, Right((pool5_, bundle22))) = action5.run(RuntimeCtx.at(compound2Height)).value

          val action5_            = pool5_.updateBudgets(compounded_epoch = 2)
          val (_, Right((pool7))) = action5_.run(RuntimeCtx.at(compound2Height)).value

          // Third epoch (1 optional budget deposit):

          val action7                       = pool7.compound(bundle12, epoch = 3)
          val (_, Right((pool8, bundle13))) = action7.run(RuntimeCtx.at(compound3Height)).value

          val action8                        = pool8.compound(bundle22, epoch = 3)
          val (_, Right((pool9_, bundle23))) = action8.run(RuntimeCtx.at(compound3Height)).value

          val action9_            = pool9_.updateBudgets(compounded_epoch = 3)
          val (_, Right((pool9))) = action9_.run(RuntimeCtx.at(compound3Height)).value

          // Program is ended, redeems:
          val action10 = pool9.redeemBudget(0)
          val (_, Right((pool10, mainBudgetOut))) =
            action10.run(RuntimeCtx.at(compound3Height + redeemLimitDelta)).value

          val action11                           = pool10.redeemBudget(1)
          val (_, Right((pool11, optBudgetOut))) = action11.run(RuntimeCtx.at(compound3Height + redeemLimitDelta)).value

          val action12                         = pool11.redeem(bundle13)
          val (_, Right((pool12, outRedeem1))) = action12.run(RuntimeCtx.at(compound3Height)).value

          val action13                         = pool12.redeem(bundle23)
          val (_, Right((pool13, outRedeem2))) = action13.run(RuntimeCtx.at(compound3Height)).value

          val poolBox0  = pool01.toLedger[Ledger]
          val poolBox1  = pool1.toLedger[Ledger]
          val poolBox2  = pool2.toLedger[Ledger]
          val poolBox3  = pool3.toLedger[Ledger]
          val poolBox33 = pool33.toLedger[Ledger]
          val poolBox3_ = pool3_.toLedger[Ledger]
          val poolBox4  = pool4.toLedger[Ledger]
          val poolBox5  = pool5.toLedger[Ledger]
          val poolBox5_ = pool5_.toLedger[Ledger]
          val poolBox7  = pool7.toLedger[Ledger]
          val poolBox8  = pool8.toLedger[Ledger]
          val poolBox9  = pool9.toLedger[Ledger]
          val poolBox9_ = pool9_.toLedger[Ledger]
          val poolBox10 = pool10.toLedger[Ledger]
          val poolBox11 = pool11.toLedger[Ledger]
          val poolBox12 = pool12.toLedger[Ledger]
          val poolBox13 = pool13.toLedger[Ledger]

          // Valid budget updates:
          val (_, isValidCompound1BudgetsUpdate) = poolBox3_.validator
            .run(
              RuntimeCtx(
                compound1Height,
                inputs  = List(poolBox3_),
                outputs = List(poolBox3)
              )
            )
            .value

          val (_, isValidCompound2BudgetsUpdate) = poolBox5_.validator
            .run(
              RuntimeCtx(
                compound2Height,
                inputs  = List(poolBox5_),
                outputs = List(poolBox7)
              )
            )
            .value

          isValidCompound1BudgetsUpdate shouldBe true
          isValidCompound2BudgetsUpdate shouldBe true

          // Valid budget deposits:
          val depositMainBudgetBox0 =
            getDepositBudgetBox("X0", mainBudgetAdd, "Host", 0)
          val (_, isValidMainBudgetDeposit) = poolBox3.validator
            .run(
              RuntimeCtx(
                compound1Height + 1,
                inputs  = List(poolBox3, depositMainBudgetBox0),
                outputs = List(poolBox33)
              )
            )
            .value

          val depositOptBudgetBox0 =
            getDepositBudgetBox("X1", optBudgetAdd, "Spectrum", 1)
          val (_, isValidOptBudgetDeposit) = poolBox33.validator
            .run(
              RuntimeCtx(
                compound1Height + 1,
                inputs  = List(poolBox33, depositOptBudgetBox0),
                outputs = List(poolBox4)
              )
            )
            .value

          isValidMainBudgetDeposit shouldBe true
          isValidOptBudgetDeposit shouldBe true

          // Valid budget redeems:
          val (redeemBudgetBox1, redeemerBudgetBox1) =
            getRedeemBudgetBoxes("X0", pool9.reserves.X0, mainBudgetOut.value, "Host", "Host")

          val (_, isValidPool9) = poolBox9.validator
            .run(
              RuntimeCtx(
                compound3Height + redeemLimitDelta,
                inputs  = List(poolBox9, redeemBudgetBox1),
                outputs = List(poolBox10, redeemerBudgetBox1)
              )
            )
            .value

          val (_, isValidBudgetRedeem1) = redeemBudgetBox1.validator
            .run(
              RuntimeCtx(
                compound3Height + redeemLimitDelta,
                inputs  = List(poolBox9, redeemBudgetBox1),
                outputs = List(poolBox10, redeemerBudgetBox1)
              )
            )
            .value

          val (redeemBudgetBox2, redeemerBudgetBox2) =
            getRedeemBudgetBoxes("X1", pool10.reserves.X1, optBudgetOut.value, "Spectrum", "Spectrum")

          val (_, isValidPool10) = poolBox10.validator
            .run(
              RuntimeCtx(
                compound3Height + redeemLimitDelta,
                inputs  = List(poolBox10, redeemBudgetBox2),
                outputs = List(poolBox11, redeemerBudgetBox2)
              )
            )
            .value

          val (_, isValidBudgetRedeem2) = redeemBudgetBox2.validator
            .run(
              RuntimeCtx(
                compound3Height + redeemLimitDelta,
                inputs  = List(poolBox10, redeemBudgetBox2),
                outputs = List(poolBox11, redeemerBudgetBox2)
              )
            )
            .value

          val shouldBeValue = if (mainBudgetOut.value > 0) true else false
          isValidPool9 shouldBe shouldBeValue
          isValidBudgetRedeem1 shouldBe true

          isValidPool10 shouldBe true
          isValidBudgetRedeem2 shouldBe true

          // Valid users' deposits:
          val (userBox1, depositBox1, bundleBox1) =
            getDepositTxBoxes(input0.value, 3, input0.value, bundle1.TMP)
          val (_, isValidDeposit0) = depositBox1.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                inputs  = List(poolBox0, depositBox1),
                outputs = List(poolBox1, userBox1, bundleBox1)
              )
            )
            .value
          val (_, isValidPool0) = poolBox0.validator
            .run(
              RuntimeCtx(
                startAtHeight,
                inputs  = List(poolBox0, depositBox1),
                outputs = List(poolBox1, userBox1, bundleBox1)
              )
            )
            .value

          val (userBox2, depositBox2, bundleBox2) =
            getDepositTxBoxes(input1.value, 2, input1.value, bundle2.TMP)
          val (_, isValidDeposit1) = depositBox2.validator
            .run(
              RuntimeCtx(
                startAtHeight + 2,
                inputs  = List(poolBox1, depositBox2),
                outputs = List(poolBox2, userBox2, bundleBox2)
              )
            )
            .value
          val (_, isValidPool1) = poolBox1.validator
            .run(
              RuntimeCtx(
                startAtHeight + 2,
                inputs  = List(poolBox1, depositBox2),
                outputs = List(poolBox2, userBox2, bundleBox2)
              )
            )
            .value

          isValidPool0 shouldBe true
          isValidPool1 shouldBe true
          isValidDeposit0 shouldBe true
          isValidDeposit1 shouldBe true

          // Valid compounds:
          val (_, bundleBox11) =
            getCompoundTxBoxes(
              bundle1.vLQ,
              bundle1.rewardMain,
              bundle1.rewardOpt,
              bundle1.TMP,
              bundle11.rewardMain,
              bundle11.rewardOpt,
              bundle11.TMP
            )

          val (_, isValidCompoundReward1) = bundleBox1.validator
            .run(
              RuntimeCtx(
                compound1Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3_, bundleBox11)
              )
            )
            .value

          val (_, isValidPool2) = poolBox2.validator
            .run(
              RuntimeCtx(
                compound1Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox2, bundleBox1),
                outputs = List(poolBox3_, bundleBox11)
              )
            )
            .value

          isValidPool2 shouldBe true
          isValidCompoundReward1 shouldBe true

          val (_, bundleBox12) =
            getCompoundTxBoxes(
              bundle11.vLQ,
              bundle11.rewardMain,
              bundle11.rewardOpt,
              bundle11.TMP,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle12.TMP
            )

          val (_, isValidCompoundReward12) = bundleBox11.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox4, bundleBox11),
                outputs = List(poolBox5, bundleBox12)
              )
            )
            .value

          val (_, isValidPool4) = poolBox4.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox4, bundleBox11),
                outputs = List(poolBox5, bundleBox12)
              )
            )
            .value

          isValidPool4 shouldBe true
          isValidCompoundReward12 shouldBe true

          val (_, bundleBox22) =
            getCompoundTxBoxes(
              bundle2.vLQ,
              bundle2.rewardMain,
              bundle2.rewardOpt,
              bundle22.TMP,
              bundle22.rewardMain,
              bundle22.rewardOpt,
              bundle22.TMP
            )

          val (_, isValidCompoundReward22) = bundleBox2.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox5, bundleBox2),
                outputs = List(poolBox5_, bundleBox22)
              )
            )
            .value

          val (_, isValidPool5) = poolBox5.validator
            .run(
              RuntimeCtx(
                compound2Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox5, bundleBox2),
                outputs = List(poolBox5_, bundleBox22)
              )
            )
            .value

          isValidPool5 shouldBe true
          isValidPool2 shouldBe true
          isValidCompoundReward22 shouldBe true

          val (_, bundleBox13) =
            getCompoundTxBoxes(
              bundle12.vLQ,
              bundle12.rewardMain,
              bundle12.rewardOpt,
              bundle13.TMP,
              bundle13.rewardMain,
              bundle13.rewardOpt,
              bundle13.TMP
            )

          val (_, isValidCompoundReward13) = bundleBox12.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox7, bundleBox12),
                outputs = List(poolBox8, bundleBox13)
              )
            )
            .value

          val (_, isValidPool7) = poolBox7.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox7, bundleBox12),
                outputs = List(poolBox8, bundleBox13)
              )
            )
            .value

          isValidPool7 shouldBe true
          isValidCompoundReward13 shouldBe true

          val (_, bundleBox23) =
            getCompoundTxBoxes(
              bundle22.vLQ,
              bundle22.rewardMain,
              bundle22.rewardOpt,
              bundle23.TMP,
              bundle23.rewardMain,
              bundle23.rewardOpt,
              bundle23.TMP
            )

          val (_, isValidCompoundReward23) = bundleBox22.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox8, bundleBox22),
                outputs = List(poolBox9_, bundleBox23)
              )
            )
            .value

          val (_, isValidPool8) = poolBox8.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 0, 1 -> 1),
                inputs  = List(poolBox8, bundleBox22),
                outputs = List(poolBox9_, bundleBox23)
              )
            )
            .value

          isValidPool8 shouldBe true
          isValidCompoundReward23 shouldBe true

          // Check users' rewards redeems:
          val (redeemRewardsBox1, userBoxRedeemRewards1) =
            getRedeemRewardsBoxes("X0", bundle13.rewardMain - 1L, "X0", bundle13.rewardMain - 1L, "user")

          val (bundleBox13toRedeem, bundleBox13empty) =
            getCompoundTxBoxes(
              bundle13.vLQ,
              bundle13.rewardMain,
              bundle13.rewardOpt,
              bundle13.TMP,
              1L,
              bundle13.rewardOpt,
              bundle13.TMP,
              actionId    = 1,
              actionIdOut = 1
            )

          val (_, isValidRedeemRewardBundle1) = bundleBox13toRedeem.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 1, 1 -> 2),
                inputs  = List(bundleBox13toRedeem, redeemRewardsBox1),
                outputs = List(bundleBox13empty, userBoxRedeemRewards1)
              )
            )
            .value

          val (_, isValidRedeemReward1) = redeemRewardsBox1.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 1, 1 -> 2),
                inputs  = List(bundleBox13toRedeem, redeemRewardsBox1),
                outputs = List(bundleBox13empty, userBoxRedeemRewards1)
              )
            )
            .value

          isValidRedeemRewardBundle1 shouldBe true
          isValidRedeemReward1 shouldBe true

          val (redeemRewardsBox11, userBoxRedeemRewards11) =
            getRedeemRewardsBoxes("X1", bundle13.rewardOpt, "X1", bundle13.rewardOpt, "user")

          val (_, bundleBox13depleted) =
            getCompoundTxBoxes(
              bundle13.vLQ,
              bundle13.rewardMain,
              bundle13.rewardOpt,
              bundle13.TMP,
              1L,
              1L,
              bundle13.TMP,
              actionId = 1
            )

          val (_, isValidRedeemRewardBundle11) = bundleBox13empty.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 1, 1 -> 3),
                inputs  = List(bundleBox13empty, redeemRewardsBox11),
                outputs = List(bundleBox13depleted, userBoxRedeemRewards11)
              )
            )
            .value

          val (_, isValidRedeemReward11) = redeemRewardsBox11.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 2, 1 -> 3),
                inputs  = List(bundleBox13empty, redeemRewardsBox11),
                outputs = List(bundleBox13depleted, userBoxRedeemRewards11)
              )
            )
            .value

          isValidRedeemRewardBundle11 shouldBe true
          isValidRedeemReward11 shouldBe true

          val (redeemRewardsBox21, userBoxRedeemRewards21) =
            getRedeemRewardsBoxes("X0", bundle23.rewardMain, "X0", bundle23.rewardMain, "user")

          val (bundleBox23toRedeem, bundleBox23empty) =
            getCompoundTxBoxes(
              bundle23.vLQ,
              bundle23.rewardMain,
              bundle23.rewardOpt,
              bundle23.TMP,
              1L,
              bundle23.rewardOpt,
              bundle23.TMP,
              actionId    = 1,
              actionIdOut = 1
            )

          val (_, isValidRedeemRewardBundle21) = bundleBox23toRedeem.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 1, 1 -> 2),
                inputs  = List(bundleBox23toRedeem, redeemRewardsBox21),
                outputs = List(bundleBox23empty, userBoxRedeemRewards21)
              )
            )
            .value

          val (_, isValidRedeemReward21) = redeemRewardsBox21.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 1, 1 -> 2),
                inputs  = List(bundleBox23toRedeem, redeemRewardsBox21),
                outputs = List(bundleBox23empty, userBoxRedeemRewards21)
              )
            )
            .value

          isValidRedeemRewardBundle21 shouldBe true
          isValidRedeemReward21 shouldBe true

          val (redeemRewardsBox22, userBoxRedeemRewards22) =
            getRedeemRewardsBoxes("X1", bundle23.rewardOpt - 1L, "X1", bundle23.rewardOpt - 1L, "user")

          val (_, bundleBox23depleted) =
            getCompoundTxBoxes(
              bundle23.vLQ,
              bundle23.rewardMain,
              bundle23.rewardOpt,
              bundle23.TMP,
              1L,
              1L,
              bundle23.TMP,
              actionId = 1
            )

          val (_, isValidRedeemRewardBundle22) = bundleBox23empty.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 1, 1 -> 3),
                inputs  = List(bundleBox23empty, redeemRewardsBox22),
                outputs = List(bundleBox23depleted, userBoxRedeemRewards22)
              )
            )
            .value

          val (_, isValidRedeemReward22) = redeemRewardsBox22.validator
            .run(
              RuntimeCtx(
                compound3Height,
                vars    = Map(0 -> 2, 1 -> 3),
                inputs  = List(bundleBox23empty, redeemRewardsBox22),
                outputs = List(bundleBox23depleted, userBoxRedeemRewards22)
              )
            )
            .value

          isValidRedeemRewardBundle22 shouldBe true
          isValidRedeemReward22 shouldBe true

          // Check users' redeems:
          val (userBoxRedeem1, redeemBox1, _) =
            getRedeemTxBoxes(bundle13.TMP, bundle13.rewardMain, bundle13.rewardOpt, bundle13.vLQ, outRedeem1.value)

          val (_, isValidBundleRedeem1) =
            bundleBox13depleted.validator
              .run(
                RuntimeCtx(
                  compound3Height + redeemLimitDelta,
                  inputs  = List(poolBox11, bundleBox13depleted, redeemBox1),
                  outputs = List(poolBox12, userBoxRedeem1)
                )
              )
              .value

          val (_, isValidRedeem1) =
            redeemBox1.validator
              .run(
                RuntimeCtx(
                  compound3Height + redeemLimitDelta,
                  inputs  = List(poolBox11, bundleBox13depleted, redeemBox1),
                  outputs = List(poolBox12, userBoxRedeem1)
                )
              )
              .value

          val (_, isValidPool11) = poolBox11.validator
            .run(
              RuntimeCtx(
                compound3Height + redeemLimitDelta,
                inputs  = List(poolBox11, bundleBox13depleted, redeemBox1),
                outputs = List(poolBox12, userBoxRedeem1)
              )
            )
            .value

          isValidRedeem1 shouldBe true
          isValidBundleRedeem1 shouldBe true
          isValidPool11 shouldBe true

          val (userBoxRedeem2, redeemBox2, _) =
            getRedeemTxBoxes(bundle23.TMP, bundle23.rewardMain, bundle23.rewardOpt, bundle23.vLQ, outRedeem2.value)

          val (_, isValidBundleRedeem2) =
            bundleBox23depleted.validator
              .run(
                RuntimeCtx(
                  compound3Height + redeemLimitDelta,
                  inputs  = List(poolBox12, bundleBox23depleted, redeemBox2),
                  outputs = List(poolBox13, userBoxRedeem2)
                )
              )
              .value

          val (_, isValidRedeem2) =
            redeemBox2.validator
              .run(
                RuntimeCtx(
                  compound3Height + redeemLimitDelta,
                  inputs  = List(poolBox12, bundleBox23depleted, redeemBox2),
                  outputs = List(poolBox13, userBoxRedeem2)
                )
              )
              .value

          val (_, isValidPool12) = poolBox12.validator
            .run(
              RuntimeCtx(
                compound3Height + redeemLimitDelta,
                inputs  = List(poolBox12, bundleBox23depleted, redeemBox2),
                outputs = List(poolBox13, userBoxRedeem2)
              )
            )
            .value

          isValidRedeem2 shouldBe true
          isValidBundleRedeem2 shouldBe true
          isValidPool12 shouldBe true

          // Check final amounts:
          pool9.reserves.X0 <= maxRoundingError shouldBe true
          pool9.reserves.X1 <= maxRoundingError shouldBe true

          (bundle23.rewardMain + bundle13.rewardMain - mainBudget - mainBudgetAdd) <= maxRoundingError shouldBe true
          (bundle23.rewardOpt + bundle13.rewardOpt - optBudget - optBudgetAdd) <= maxRoundingError shouldBe true

          pool13.reserves.X0 <= maxRoundingError shouldBe true
          pool13.reserves.X1 <= maxRoundingError shouldBe true
          pool13.reserves.vLQ == pool01.reserves.vLQ shouldBe true
          pool13.reserves.TMP == pool01.reserves.TMP shouldBe true
        }
      }
    }
  }
}
