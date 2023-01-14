package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.Helpers.{boxId, bytes}
import io.ergodex.core.ToLedger._
import io.ergodex.core.cfmm3.UserBox
import io.ergodex.core.cfmm3.t2t.CfmmPool._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SwapBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 100L

  def getBoxes(
    swappedAmount: Long,
    expectedAmount: Long,
    swappedToken: String,
    recToken: String
  ): (UserBox[Ledger], SwapBox[Ledger], UserBox[Ledger]) = {
    val spfIsQuote = if (recToken == "spf") true else false
    val userBox    = new UserBox(
      boxId("redeemer_box"),
      0L,
      DefaultCreationHeight,
      tokens         = Vector(bytes(recToken) -> expectedAmount, bytes("spf") -> expectedAmount),
      registers      = Map.empty,
      constants      = Map.empty,
      validatorBytes = "redeemer"
    )

    val swapBox = new SwapBox(
      boxId("swap_box"),
      0L,
      DefaultCreationHeight,
      tokens         = Vector(
        bytes(swappedToken) -> swappedAmount
      ),
      registers      = Map.empty,
      constants      = Map(
        1  -> bytes(recToken),
        2  -> false,
        7  -> spfIsQuote,
        8  -> 1400L,
        12 -> 1200L,
        13 -> 996,
        17 -> bytes("pool_NFT"),
        18 -> bytes("redeemer"),
        19 -> 800L,
        23 -> 22L,
        24 -> 100L,
        27 -> bytes("spf"),
        29 -> 1000,
        31 -> bytes("miner"),
        34 -> minerFee
      ),
      validatorBytes = "swap"
    )

    val minerBox = new UserBox(
      boxId("miner_box"),
      minerFee,
      DefaultCreationHeight,
      tokens         = Vector(),
      registers      = Map.empty,
      constants      = Map.empty,
      validatorBytes = "miner"
    )

    (userBox, swapBox, minerBox)
  }

  val pool01: CfmmPool[Ledger] = {
    val inX               = 1000000000L
    val inY               = 1000000000L
    val feeNum            = 996
    val feeDenom          = 1000
    val emissionLP        = Long.MaxValue
    val burnLP            = 100000L
    val minInitialDeposit = 100L

    val conf = PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, burnLP: Long, minInitialDeposit: Long)

    CfmmPool.init(inX, inY, conf)
  }

  it should "validate swap y behaviour mirrored from simulation" in {
    val startAtHeight               = 101
    val inputY: AssetInput[Token.Y] = AssetInput(1000000)

    val action                         = pool01.swapY(inputY)
    val (_, Right((pool1, receivedX))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedXAmount = receivedX.value

    val poolBox0                        = pool01.toLedger[Ledger]
    val poolBox1                        = pool1.toLedger[Ledger]
    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, expectedXAmount, "y", "x")

    val (_, isValidSwapY) = swapBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox0, swapBox1),
          outputs = List(poolBox1, userBox1, minerBox1),
          vars    = Map(0 -> 0, 1 -> false)
        )
      )
      .value
    val (_, isValidPool)  = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }

  it should "validate swap x behaviour mirrored from simulation" in {
    val startAtHeight               = 101
    val inputX: AssetInput[Token.X] = AssetInput(2000011)

    val action                         = pool01.swapX(inputX)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputX.value, expectedYAmount, "x", "y")

    val (_, isValidSwapY) = swapBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox0, swapBox1),
          outputs = List(poolBox1, userBox1, minerBox1),
          vars    = Map(0 -> 1, 1 -> false)
        )
      )
      .value
    val (_, isValidPool)  = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }

  it should "validate illegal swap behaviour" in {
    val startAtHeight               = 101
    val inputX: AssetInput[Token.X] = AssetInput(300023)

    val action                         = pool01.swapX(inputX)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputX.value, 1, "x", "y")

    val (_, isValidSwapY) = swapBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox0, swapBox1),
          outputs = List(poolBox1, userBox1, minerBox1)
        )
      )
      .value
    val (_, isValidPool)  = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe false
    isValidPool shouldBe true
  }

  it should "validate swap behaviour then x is spf mirrored from simulation" in {
    val startAtHeight               = 101
    val inputX: AssetInput[Token.X] = AssetInput(300023)

    val action                         = pool01.swapX(inputX)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputX.value, expectedYAmount, "x", "y")

    val (_, isValidSwapY) = swapBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox0, swapBox1),
          outputs = List(poolBox1, userBox1, minerBox1)
        )
      )
      .value
    val (_, isValidPool)  = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }

  it should "validate swap behaviour then y is spf mirrored from simulation" in {
    val startAtHeight               = 101
    val inputY: AssetInput[Token.Y] = AssetInput(30137)

    val action                         = pool01.swapY(inputY)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, expectedYAmount, "spf", "x")

    val (_, isValidSwapY) = swapBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox0, swapBox1),
          outputs = List(poolBox1, userBox1, minerBox1),
          vars    = Map(0 -> 0, 1 -> true)
        )
      )
      .value
    val (_, isValidPool)  = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }

  it should "validate illegal swap behaviour then y is spf mirrored from simulation" in {
    val startAtHeight               = 101
    val inputY: AssetInput[Token.Y] = AssetInput(30137)

    val action                         = pool01.swapY(inputY)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, expectedYAmount - 1, "spf", "x")

    val (_, isValidSwapY) = swapBox1.validator
      .run(
        RuntimeCtx(
          startAtHeight,
          inputs  = List(poolBox0, swapBox1),
          outputs = List(poolBox1, userBox1, minerBox1)
        )
      )
      .value
    val (_, isValidPool)  = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }
}
