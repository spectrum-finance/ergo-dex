package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.Helpers.{boxId, tokenId}
import io.ergodex.core.ToLedger._
import io.ergodex.core.cfmm3.n2t.CfmmPool._
import io.ergodex.core.cfmm3.{MinerBox, UserBox}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks


class DepositBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 10L
  val exFee = 10L

  def getBoxes(depositedXAmount: Long, depositedYAmount: Long,
               expectedLPAmount: Long, changeAmount: Long, changeIsY: Boolean = false): (UserBox[Ledger], DepositBox[Ledger], MinerBox[Ledger]) = {

    val userBox = if (changeIsY) {
      new UserBox(
        boxId("redeemer_box"),
        0,
        DefaultCreationHeight,
        tokens = Vector(
          tokenId("lp") -> expectedLPAmount,
          tokenId("y") -> changeAmount,
        ),
        registers = Map(
        )
      )
    }
    else {
      new UserBox(
        boxId("redeemer_box"),
        changeAmount,
        DefaultCreationHeight,
        tokens = Vector(
          tokenId("lp") -> expectedLPAmount,
        ),
        registers = Map(
        )
      )
    }

    val depositBox = new DepositBox(
      boxId("deposit_box"),
      depositedXAmount - exFee,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("y") -> depositedYAmount,
      ),
      registers = Map()
    )

    val minerBox = new MinerBox(
      boxId("miner_box"),
      minerFee,
      DefaultCreationHeight,
      tokens = Vector(),
      registers = Map()
    )

    (userBox, depositBox, minerBox)
  }

  val pool01: CfmmPool[Ledger] = {
    val inX = 100L
    val inY = 100L
    val feeNum = 997
    val feeDenom = 1000
    val emissionLP = Long.MaxValue
    val burnLP = 10000L
    val minInitialDeposit = 100L

    val conf = PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, burnLP: Long, minInitialDeposit: Long)

    CfmmPool.init(inX, inY, conf)
  }


  it should "validate equal deposit behaviour mirrored from simulation" in {
    val startAtHeight = 101
    val inputX: Long = 1000
    val inputY: AssetInput[Token.Y] = AssetInput(1000 - exFee)

    val action = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, changeX))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedLPAmount = receivedLP.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX, inputY.value, expectedLPAmount, changeX.value)


    val (_, isValidDeposit) = depositBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> false))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    changeX.value shouldBe inputX - inputY.value
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour than x exceeds y mirrored from simulation" in {
    val startAtHeight = 101
    val inputX: Long = 110
    val inputY: AssetInput[Token.Y] = AssetInput(100 - exFee)

    val action = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, changeX))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedLPAmount = receivedLP.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX, inputY.value, expectedLPAmount, changeX.value)


    val (_, isValidDeposit) = depositBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> false))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    changeX.value shouldBe inputX - inputY.value
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour than y exceeds x from simulation" in {
    val startAtHeight = 101
    val inputX: Long = 110003
    val inputY: AssetInput[Token.Y] = AssetInput(1350000)

    val action = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, changeY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedLPAmount = receivedLP.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX, inputY.value - exFee, expectedLPAmount, changeY.value, changeIsY = true)


    val (_, isValidDeposit) = depositBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> false))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    changeY.value shouldBe inputY.value - inputX
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour than y is spf from simulation" in {
    val startAtHeight = 101
    val inputX: Long = 110003
    val inputY: AssetInput[Token.Y] = AssetInput(1350000)

    val action = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, changeY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedLPAmount = receivedLP.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX, inputY.value, expectedLPAmount, changeY.value, changeIsY = true)


    val (_, isValidDeposit) = depositBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> true))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    changeY.value shouldBe inputY.value - inputX
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }
}