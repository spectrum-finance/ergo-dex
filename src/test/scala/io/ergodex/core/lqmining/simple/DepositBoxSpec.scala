package io.ergodex.core.lqmining.simple

import io.ergodex.core.Helpers.{boxId, tokenId}
import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.simple.LMPool._
import io.ergodex.core.lqmining.simple.Token._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import io.ergodex.core.syntax.blake2b256
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DepositBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  def getBoxes(
    depositedLQAmount: Long,
    expectedNumEpochs: Int,
    expectedVLQAmount: Long,
    expectedTMPAmount: Long,
    bundleValidatorBytesTag: String = "staking_bundle"
  ): (UserBox[Ledger], DepositBox[Ledger], StakingBundleBox[Ledger]) = {

    val userBox = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("lm_pool_id") -> BundleKeyTokenAmount
      ),
      registers = Map.empty
    )

    val depositBox = new DepositBox(
      boxId("deposit_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("LQ") -> depositedLQAmount
      ),
      registers = Map.empty,
      constants = Map(
        1  -> tokenId("LM_Pool_NFT_ID"),
        3  -> tokenId("user"),
        6  -> false,
        10 -> blake2b256(bundleValidatorBytesTag.getBytes().toVector),
        14 -> expectedNumEpochs,
        18 -> tokenId("miner"),
        21 -> 100L
      ),
      validatorBytes = "deposit_order"
    )

    val bundleBox = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("vLQ")        -> expectedVLQAmount,
        tokenId("TMP")        -> expectedTMPAmount,
        tokenId("lm_pool_id") -> 1L
      ),
      registers = Map(
        4 -> tokenId("user"),
        5 -> tokenId("LM_Pool_NFT_ID")
      )
    )

    (userBox, depositBox, bundleBox)
  }

  val epochNum         = 4
  val epochLen         = 3
  val programStart     = 2
  val redeemDelta      = 10
  val maxRoundingError = 1000L

  val pool01: LMPool[Ledger] =
    LMPool.init(
      epochLen,
      epochNum,
      programStart,
      redeemDelta,
      programBudget = 900000000L * maxRoundingError,
      maxRoundingError
    )

  val depositedLQAmount      = 100000L * maxRoundingError // 1L, 1000000L
  val input0: AssetInput[LQ] = AssetInput(depositedLQAmount)

  it should "validate deposit behaviour before LM program start mirrored from simulation" in {
    val expectedNumEpochs            = epochNum
    val startAtHeight                = programStart - epochLen * 3
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedVLQAmount = depositedLQAmount
    val expectedTMPAmount = depositedLQAmount * expectedNumEpochs

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, bundleBox1) =
      getBoxes(depositedLQAmount, expectedNumEpochs, expectedVLQAmount, expectedTMPAmount)

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = List(poolBox0), outputs = List(poolBox1, userBox1, bundleBox1)))
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour at first epoch of LM program mirrored from simulation" in {
    val expectedNumEpochs            = epochNum - 1
    val startAtHeight                = programStart + epochLen - 1
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedVLQAmount = depositedLQAmount
    val expectedTMPAmount = depositedLQAmount * expectedNumEpochs

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, bundleBox1) =
      getBoxes(depositedLQAmount, expectedNumEpochs, expectedVLQAmount, expectedTMPAmount)

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = List(poolBox0), outputs = List(poolBox1, userBox1, bundleBox1)))
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate invalid deposit behaviour than bundle script is not preserved mirrored from simulation" in {
    val expectedNumEpochs            = epochNum - 1
    val startAtHeight                = programStart + epochLen - 1
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedVLQAmount = depositedLQAmount
    val expectedTMPAmount = depositedLQAmount * expectedNumEpochs

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, bundleBox1) = getBoxes(
      depositedLQAmount,
      expectedNumEpochs,
      expectedVLQAmount,
      expectedTMPAmount,
      bundleValidatorBytesTag = "bad_box"
    )

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = List(poolBox0), outputs = List(poolBox1, userBox1, bundleBox1)))
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe false
    isValidPool shouldBe true
  }
}
