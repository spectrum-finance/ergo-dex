package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.Helpers.{boxId, tokenId}
import io.ergodex.core.ToLedger._
import io.ergodex.core.cfmm3.n2t.CfmmPool._
import io.ergodex.core.cfmm3.{MinerBox, UserBox}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks


class SwapBuyBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 10L

  def getBoxes(swappedAmount: Long, expectedAmount: Long, swappedToken: String): (UserBox[Ledger], SwapBuyBox[Ledger], MinerBox[Ledger]) = {
    val userBox =
      new UserBox(
        boxId("redeemer_box"),
        expectedAmount,
        DefaultCreationHeight,
        tokens = Vector(tokenId("y") -> expectedAmount),
        registers = Map(
        )
      )
    val swapBuyBox =
      new SwapBuyBox(
        boxId("deposit_box"),
        0L,
        DefaultCreationHeight,
        tokens = Vector(tokenId(swappedToken) -> swappedAmount),
        registers = Map()
      )
    val minerBox = new MinerBox(
      boxId("miner_box"),
      minerFee,
      DefaultCreationHeight,
      tokens = Vector(),
      registers = Map()
    )

    (userBox, swapBuyBox, minerBox)
  }

  val pool01: CfmmPool[Ledger] = {
    val inX = 10000000L
    val inY = 10000000L
    val feeNum = 996
    val feeDenom = 1000
    val emissionLP = Long.MaxValue
    val burnLP = 100000L
    val minInitialDeposit = 100L

    val conf = PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, burnLP: Long, minInitialDeposit: Long)

    CfmmPool.init(inX, inY, conf)
  }


  it should "validate swap y behaviour mirrored from simulation" in {
    val startAtHeight = 101
    val inputY: AssetInput[Token.Y] = AssetInput(1000000)

    val action = pool01.swapY(inputY)
    val (_, Right((pool1, receivedX))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedXAmount = receivedX

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]
    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, expectedXAmount, "y")

    val (_, isValidSwapY) = swapBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1),
      outputs = List(poolBox1, userBox1, minerBox1))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe true
    isValidPool shouldBe true
  }


  it should "validate illegal swap behaviour" in {
    val startAtHeight = 101
    val inputY: AssetInput[Token.Y] = AssetInput(300023)

    val action = pool01.swapY(inputY)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedXAmount = receivedY

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputY.value, expectedXAmount - 1, "y")


    val (_, isValidSwapY) = swapBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1),
      outputs = List(poolBox1, userBox1, minerBox1))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapY shouldBe false
    isValidPool shouldBe true
  }
}