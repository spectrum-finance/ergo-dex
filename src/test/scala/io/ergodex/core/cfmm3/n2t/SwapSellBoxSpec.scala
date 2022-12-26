package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.Helpers.{boxId, tokenId}
import io.ergodex.core.ToLedger._
import io.ergodex.core.cfmm3.n2t.CfmmPool._
import io.ergodex.core.cfmm3.{MinerBox, UserBox}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks


class SwapSellBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 10L

  def getBoxes(swappedAmount: Long, expectedAmount: Long, tokenY: String): (UserBox[Ledger], SwapSellBox[Ledger], MinerBox[Ledger]) = {
    val userBox =
      new UserBox(
        boxId("redeemer_box"),
        expectedAmount,
        DefaultCreationHeight,
        tokens = Vector(tokenId(tokenY) -> expectedAmount),
        registers = Map()
      )
    val swapSellBox =
      new SwapSellBox(
        boxId("deposit_box"),
        swappedAmount,
        DefaultCreationHeight,
        tokens = Vector(tokenId(tokenY) -> swappedAmount),
        registers = Map(4 -> tokenId(tokenY))
      )
    val minerBox = new MinerBox(
      boxId("miner_box"),
      minerFee,
      DefaultCreationHeight,
      tokens = Vector(),
      registers = Map()
    )

    (userBox, swapSellBox, minerBox)
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


  it should "validate swap x behaviour mirrored from simulation" in {
    val startAtHeight = 101
    val inputX = 1000000

    val action = pool01.swapX(inputX)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]
    val (userBox1, swapBox1, minerBox1) = getBoxes(inputX, expectedYAmount, "y")

    val (_, isValidSwapX) = swapBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> false))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapX shouldBe true
    isValidPool shouldBe true
  }


  it should "validate illegal swap behaviour" in {
    val startAtHeight = 101
    val inputX = 300023

    val action = pool01.swapX(inputX)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, swapBox1, minerBox1) = getBoxes(inputX, expectedYAmount - 1, "y")


    val (_, isValidSwapX) = swapBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> false))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapX shouldBe false
    isValidPool shouldBe true
  }

  it should "validate swap x behaviour when y is spf mirrored from simulation" in {
    val startAtHeight = 101
    val inputX = 839843893

    val action = pool01.swapX(inputX)
    val (_, Right((pool1, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]
    val (userBox1, swapBox1, minerBox1) = getBoxes(inputX, expectedYAmount, "y")

    val (_, isValidSwapX) = swapBox1.validator.run(RuntimeCtx(startAtHeight, inputs = List(poolBox0, swapBox1),
      outputs = List(poolBox1, userBox1, minerBox1), vars = Map(0 -> true))).value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidSwapX shouldBe true
    isValidPool shouldBe true
  }
}