package io.ergodex.core.lqmining.simple

import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.simple.LMPool._
import io.ergodex.core.lqmining.simple.Token._
import io.ergodex.core.lqmining.simple.TxBoxes._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class StakingBundleBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val maxRoundingError = 1000L
  val redeemDelta      = 10
  val epochReg         = 8
  val pool01: LMPool[Ledger] =
    LMPool.init(
      epochLen     = 3,
      epochNum     = 4,
      programStart = 2,
      redeemDelta,
      programBudget = 900000000L * maxRoundingError,
      maxRoundingError
    )

  val input0: AssetInput[LQ] = AssetInput(1000 * maxRoundingError)

  it should "validate compound and redeem behaviour" in {
    val startAtHeight                        = 5
    val action                               = pool01.deposit(input0)
    val (_, Right((pool1, bundle1)))         = action.run(RuntimeCtx.at(0)).value
    val action1                              = pool1.compound(bundle1, epoch = 1)
    val (_, Right((pool2, bundle2, reward))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val action2                              = pool2.redeem(bundle2)
    val (_, Right((pool3, out3)))            = action2.run(RuntimeCtx.at(startAtHeight)).value

    val poolBox1 = pool1.toLedger[Ledger]
    val poolBox2 = pool2.toLedger[Ledger]
    val poolBox3 = pool3.toLedger[Ledger]

    val (userBoxReward, _, bundleBox1, bundleBox2) =
      getCompoundTxBoxes(reward.value, bundle1.vLQ, bundle1.TMP, bundle2.TMP)

    val (userBoxRedeemed, redeemBox) = getRedeemTxBoxes(bundle2.vLQ, out3.value)

    val (_, isValidCompoundReward) = bundleBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          vars    = Map(0 -> 1, 1 -> 2),
          inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
          outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward, bundleBox2)
        )
      )
      .value

    val (_, isValidPool) = poolBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          vars    = Map(0 -> 1, 1 -> 2),
          inputs  = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
          outputs = List(poolBox2.setRegister(epochReg, 1), userBoxReward, bundleBox2)
        )
      )
      .value

    val (_, isValidCompoundRedeem) = bundleBox2.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox2, bundleBox2, redeemBox),
          outputs = List(poolBox3, userBoxRedeemed)
        )
      )
      .value

    val (_, isValidPoolRedeemed) = poolBox2.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox3))).value

    isValidPool shouldBe true
    isValidCompoundReward shouldBe true

    isValidPoolRedeemed shouldBe true
    isValidCompoundRedeem shouldBe true
  }

  it should "validate illegal compound and redeem behaviour mirrored from simulation" in {
    val startAtHeight                = 5
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(0)).value
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

  it should "validate late compound behaviour" in {
    val startAtHeight                         = pool01.conf.programStart + pool01.conf.epochLen * 3
    val action                                = pool01.deposit(input0)
    val (_, Right((pool1, bundle1)))          = action.run(RuntimeCtx.at(0)).value
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
