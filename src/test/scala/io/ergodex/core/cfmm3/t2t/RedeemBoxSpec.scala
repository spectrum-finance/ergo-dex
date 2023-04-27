package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.Helpers.{boxId, bytes, hex, tokenId}
import io.ergodex.core.ToLedger._
import io.ergodex.core.cfmm3.UserBox
import io.ergodex.core.cfmm3.t2t.CfmmPool._
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class RedeemBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minerFee = 10L

  def getBoxes(
    redeemedLPAmount: Long,
    expectedXAmount: Long,
    expectedYAmount: Long
  ): (UserBox[Ledger], RedeemBox[Ledger], UserBox[Ledger]) = {

    val userBox = new UserBox(
      boxId("redeemer_box"),
      expectedXAmount,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("x") -> expectedXAmount,
        tokenId("y") -> expectedYAmount
      ),
      registers      = Map.empty,
      constants      = Map.empty,
      validatorBytes = hex("redeemer")
    )

    val redeemBox = new RedeemBox(
      boxId("redeem_box"),
      0L,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("lp") -> redeemedLPAmount
      ),
      registers = Map.empty,
      constants = Map(
        2  -> false,
        14 -> tokenId("pool_NFT"),
        15 -> bytes("redeemer"),
        20 -> bytes("miner"),
        23 -> minerFee
      ),
      validatorBytes = hex("redeem")
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

    (userBox, redeemBox, minerBox)
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

  it should "validate redeem behaviour mirrored from simulation" in {
    val startAtHeight                 = 101
    val inputLP: AssetInput[Token.LP] = AssetInput(1)

    val action                                    = pool01.redeem(inputLP)
    val (_, Right((pool1, receivedX, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value
    val expectedXAmount = receivedX.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, redeemBox1, minerBox1) = getBoxes(inputLP.value, expectedXAmount, expectedYAmount)

    val (_, isValidRedeem) = redeemBox1.validator
      .run(
        RuntimeCtx(startAtHeight, inputs = List(poolBox0, redeemBox1), outputs = List(poolBox1, userBox1, minerBox1))
      )
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    isValidRedeem shouldBe true
    isValidPool shouldBe true
  }

  it should "validate redeem all behaviour mirrored from simulation" in {
    val startAtHeight = 101
    val totalX        = pool01.reserves.x
    val totalY        = pool01.reserves.y

    val inputLP: AssetInput[Token.LP] = AssetInput(pool01.supplyLP)

    val action                                    = pool01.redeem(inputLP)
    val (_, Right((pool1, receivedX, receivedY))) = action.run(RuntimeCtx.at(startAtHeight)).value

    val expectedYAmount = receivedY.value
    val expectedXAmount = receivedX.value

    val poolBox0 = pool01.toLedger[Ledger]
    val poolBox1 = pool1.toLedger[Ledger]

    val (userBox1, redeemBox1, minerBox1) = getBoxes(inputLP.value, expectedXAmount, expectedYAmount)

    val (_, isValidRedeem) = redeemBox1.validator
      .run(
        RuntimeCtx(startAtHeight, inputs = List(poolBox0, redeemBox1), outputs = List(poolBox1, userBox1, minerBox1))
      )
      .value
    val (_, isValidPool) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value

    receivedX.value shouldBe totalX
    receivedY.value shouldBe totalY
    isValidRedeem shouldBe true
    isValidPool shouldBe true
  }
}
