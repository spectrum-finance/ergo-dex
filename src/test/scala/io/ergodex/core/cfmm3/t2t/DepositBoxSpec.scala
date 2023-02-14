package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.Helpers.{boxId, bytes, hex, tokenId}
import io.ergodex.core.ToLedger.ToLedgerOps
import io.ergodex.core.cfmm3.UserBox
import io.ergodex.core.cfmm3.t2t.CfmmPool._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DepositBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 10L

  def getBoxes(
    depositedXAmount: Long,
    depositedYAmount: Long,
    expectedLPAmount: Long,
    changeToken: String = "x",
    changeAmount: Long  = 0L
  ): (UserBox[Ledger], DepositBox[Ledger], UserBox[Ledger]) = {

    val userBox = new UserBox(
      boxId("redeemer_box"),
      0L,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("lp")        -> expectedLPAmount,
        tokenId(changeToken) -> changeAmount
      ),
      registers      = Map.empty,
      constants      = Map.empty,
      validatorBytes = hex("redeemer")
    )

    val depositBox = new DepositBox(
      boxId("deposit_box"),
      0L,
      DefaultCreationHeight,
      tokens    = Vector(),
      registers = Map.empty,
      constants = Map(
        1  -> false,
        8  -> depositedXAmount,
        10 -> depositedYAmount,
        13 -> tokenId("pool_NFT"),
        14 -> bytes("redeemer"),
        21 -> bytes("miner"),
        24 -> minerFee
      ),
      validatorBytes = "deposit"
    )

    val minerBox = new UserBox(
      boxId("miner_box"),
      minerFee,
      DefaultCreationHeight,
      tokens         = Vector(),
      registers      = Map.empty,
      constants      = Map.empty,
      validatorBytes = hex("miner")
    )

    (userBox, depositBox, minerBox)
  }

  val pool01: CfmmPool[Ledger] = {
    val inX               = 100L
    val inY               = 100L
    val feeNum            = 997
    val feeDenom          = 1000
    val emissionLP        = Long.MaxValue
    val burnLP            = 10000L
    val minInitialDeposit = 100L

    val conf = PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, burnLP: Long, minInitialDeposit: Long)

    CfmmPool.init(inX, inY, conf)
  }

  it should "validate deposit equal amount mirrored from simulation" in {
    val startAtHeight               = 101
    val inputX: AssetInput[Token.X] = AssetInput(100)
    val inputY: AssetInput[Token.Y] = AssetInput(100)

    val action                             = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, _))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedLPAmount = receivedLP.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX.value, inputY.value, expectedLPAmount)

    val (_, isValidDeposit) = depositBox1.validator
      .run(
        RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1), outputs = List(poolBox1, userBox1, minerBox1))
      )
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour than x exceeds y mirrored from simulation" in {
    val startAtHeight               = 101
    val inputX: AssetInput[Token.X] = AssetInput(110)
    val inputY: AssetInput[Token.Y] = AssetInput(100)

    val action                                   = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, changeX))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val expectedLPAmount                         = receivedLP.value

    val poolBox0 = pool01.toLedger
    val poolBox1 = pool1.toLedger

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX.value, inputY.value, expectedLPAmount, "x", changeX.value)

    val (_, isValidDeposit) = depositBox1.validator
      .run(
        RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1), outputs = List(poolBox1, userBox1, minerBox1))
      )
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    changeX.value shouldBe inputX.value - inputY.value
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }

  it should "validate deposit behaviour than y exceeds x from simulation" in {
    val startAtHeight               = 101
    val inputX: AssetInput[Token.X] = AssetInput(110003)
    val inputY: AssetInput[Token.Y] = AssetInput(1350000)

    val action                                   = pool01.deposit(inputX, inputY)
    val (_, Right((pool1, receivedLP, changeY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedLPAmount = receivedLP.value

    val poolBox0 = pool01.toLedger
    val poolBox1 = pool1.toLedger

    val (userBox1, depositBox1, minerBox1) = getBoxes(inputX.value, inputY.value, expectedLPAmount, "y", changeY.value)

    val (_, isValidDeposit) = depositBox1.validator
      .run(
        RuntimeCtx(startAtHeight, inputs = List(poolBox0, depositBox1), outputs = List(poolBox1, userBox1, minerBox1))
      )
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    changeY.value shouldBe inputY.value - inputX.value
    isValidDeposit shouldBe true
    isValidPool shouldBe true
  }
}
