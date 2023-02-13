package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.Helpers.{boxId, tokenId}
import io.ergodex.core.ToLedger._
import io.ergodex.core.cfmm3.UserBox
import io.ergodex.core.cfmm3.n2t.CfmmPool._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SwapBuyBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 1000L

  def getBoxes(
    swappedAmount: Long,
    expectedAmount: Long,
    swappedToken: String
  ): (UserBox[Ledger], SwapBuyBox[Ledger], UserBox[Ledger]) = {
    val userBox =
      new UserBox(
        boxId("redeemer_box"),
        expectedAmount,
        DefaultCreationHeight,
        tokens         = Vector(tokenId("y") -> expectedAmount),
        registers      = Map.empty,
        constants      = Map.empty,
        validatorBytes = "redeemer"
      )
    val swapBuyBox =
      new SwapBuyBox(
        boxId("swapBuy_box"),
        0L,
        DefaultCreationHeight,
        tokens    = Vector(tokenId(swappedToken) -> swappedAmount),
        registers = Map.empty,
        constants = Map(
          1  -> 1200L,
          2  -> 996,
          3  -> false,
          7  -> 1400L,
          8  -> 22L,
          9  -> 100L,
          11 -> tokenId("pool_NFT"),
          12 -> tokenId("redeemer"),
          13 -> 800L,
          16 -> tokenId("spf"),
          20 -> 1000,
          21 -> tokenId("miner"),
          24 -> minerFee
        ),
        validatorBytes = "swapBuy"
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

    (userBox, swapBuyBox, minerBox)
  }

  val pool01: CfmmPool[Ledger] = {
    val inX               = 10000000L
    val inY               = 10000000L
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

    val expectedXAmount = receivedX

    val poolBox0                        = pool01.toLedger[Ledger]
    val poolBox1                        = pool1.toLedger[Ledger]
    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, expectedXAmount, "y")

    val (_, isValidSwapY) = swapBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1), outputs = List(poolBox1, userBox1, minerBox1)))
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }

  it should "validate illegal swap behaviour" in {
    val startAtHeight               = 101
    val inputY: AssetInput[Token.Y] = AssetInput(3023)

    val action                         = pool01.swapY(inputY)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, 1, "y")

    val (_, isValidSwapY) = swapBox1.validator
      .run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1), outputs = List(poolBox1, userBox1, minerBox1)))
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe false
    isValidPool shouldBe true
  }
}
