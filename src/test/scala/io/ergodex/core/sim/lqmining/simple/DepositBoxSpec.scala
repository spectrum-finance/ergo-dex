package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.Helpers.{boxId, tokenId}
import io.ergodex.core.sim.ToLedger._
import io.ergodex.core.sim.lqmining.simple.LMPool._
import io.ergodex.core.sim.lqmining.simple.Token._
import io.ergodex.core.sim.{LedgerPlatform, RuntimeCtx, SigmaProp}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks


class DepositBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  def getBoxes(depositedLQAmount: Long, expectedNumEpochs: Int,
               expectedVLQAmount: Long, expectedTMPAmount: Long): (UserBox[Ledger],
    DepositBox[Ledger], StakingBundleBox[Ledger]) = {

    val userBox = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("lm_pool_id") -> 0x7fffffffffffffffL,
      ),
      registers = Map(
      )
    )

    val depositBox = new DepositBox(
      boxId("deposit_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("LQ") -> depositedLQAmount,
      ),
      registers = Map(
        5 -> expectedNumEpochs,
      )
    )

    val bundleBox = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("vLQ") -> expectedVLQAmount,
        tokenId("TMP") -> expectedTMPAmount
      ),
      registers = Map(
        4 -> tokenId("user"),
        5 -> tokenId("lm_pool_id"),
        6 -> tokenId("LM_Pool_NFT_ID"),
      )
    )

    (userBox, depositBox, bundleBox)
  }

  val epochNum = 4
  val epochLen = 3
  val programStart = 2
  val redeemDelta = 10
  val maxRoundingError = 1000L

  val pool01: LMPool[Ledger] = {
    LMPool.init(epochLen, epochNum, programStart, redeemDelta, programBudget = 900000000L * maxRoundingError, maxRoundingError)
  }


  val depositedLQAmount = 100000L * maxRoundingError // 1L, 1000000L
  val input0: AssetInput[LQ] = AssetInput(depositedLQAmount)


  it should "validate deposit behaviour before LM program start mirrored from simulation" in {
    val expectedNumEpochs = epochNum
    val startAtHeight = programStart - epochLen * 3
    val action = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedVLQAmount = depositedLQAmount
    val expectedTMPAmount = depositedLQAmount * expectedNumEpochs

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, bundleBox1) = getBoxes(depositedLQAmount, expectedNumEpochs,
      expectedVLQAmount, expectedTMPAmount)


    val (_, isValidDeposit) = depositBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0),
      outputs = List(poolBox1, userBox1, bundleBox1))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour at first epoch of LM program mirrored from simulation" in {
    val expectedNumEpochs = epochNum - 1
    val startAtHeight = programStart + epochLen - 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedVLQAmount = depositedLQAmount
    val expectedTMPAmount = depositedLQAmount * expectedNumEpochs

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, bundleBox1) = getBoxes(depositedLQAmount, expectedNumEpochs,
      expectedVLQAmount, expectedTMPAmount)


    val (_, isValidDeposit) = depositBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0),
      outputs = List(poolBox1, userBox1, bundleBox1))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }
}