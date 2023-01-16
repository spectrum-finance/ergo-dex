package io.ergodex.core.lqmining.simple

import io.ergodex.core.Helpers.bytes
import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.simple.LMPool._
import io.ergodex.core.lqmining.simple.Token._
import io.ergodex.core.lqmining.simple.TxBoxes._
import io.ergodex.core.syntax.SigmaProp
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DepositBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

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
      getDepositTxBoxes(depositedLQAmount, expectedNumEpochs, expectedVLQAmount, expectedTMPAmount)

    val txInputs  = List(poolBox0)
    val txOutputs = List(poolBox1, userBox1, bundleBox1)

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value
    val (_, isValidPool) = poolBox0.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value

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
      getDepositTxBoxes(depositedLQAmount, expectedNumEpochs, expectedVLQAmount, expectedTMPAmount)

    val txInputs  = List(poolBox0)
    val txOutputs = List(poolBox1, userBox1, bundleBox1)

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value
    val (_, isValidPool) = poolBox0.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value

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

    val (userBox1, depositBox1, bundleBox1) = getDepositTxBoxes(
      depositedLQAmount,
      expectedNumEpochs,
      expectedVLQAmount,
      expectedTMPAmount,
      bundleValidatorBytesTag = "bad_box"
    )

    val txInputs  = List(poolBox0)
    val txOutputs = List(poolBox1, userBox1, bundleBox1)

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value
    val (_, isValidPool) = poolBox0.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe false
    isValidPool shouldBe true
  }

  it should "validate invalid deposit behaviour than illegal PK stored in bundle mirrored from simulation" in {
    val expectedNumEpochs            = epochNum - 1
    val startAtHeight                = programStart + epochLen - 1
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedVLQAmount = depositedLQAmount
    val expectedTMPAmount = depositedLQAmount * expectedNumEpochs

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, bundleBox1) = getDepositTxBoxes(
      depositedLQAmount,
      expectedNumEpochs,
      expectedVLQAmount,
      expectedTMPAmount,
      redeemerProp = SigmaProp(bytes("bad_user"))
    )

    val txInputs  = List(poolBox0)
    val txOutputs = List(poolBox1, userBox1, bundleBox1)

    val (_, isValidDeposit) = depositBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value
    val (_, isValidPool) = poolBox0.validator
      .run(RuntimeCtx(startAtHeight, inputs = txInputs, outputs = txOutputs))
      .value

    bundle1.vLQ shouldBe expectedVLQAmount
    bundle1.TMP shouldBe expectedTMPAmount
    isValidDeposit shouldBe false
    isValidPool shouldBe true
  }
}
