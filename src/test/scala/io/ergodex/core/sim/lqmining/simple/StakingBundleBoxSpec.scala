package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.Helpers.{boxId, tokenId}
import io.ergodex.core.sim.ToLedger._
import io.ergodex.core.sim.lqmining.simple.LMPool._
import io.ergodex.core.sim.lqmining.simple.Token._
import io.ergodex.core.sim.{LedgerPlatform, RuntimeCtx, SigmaProp}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class StakingBundleBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  val minValue = 1000L
  val pool01: LMPool[Ledger] =
    LMPool.init(epochLen = 3, epochNum = 4, programStart = 2, programBudget = 900000000L * minValue, minValue)

  val input0: AssetInput[LQ] = AssetInput(1000 * minValue)

  it should "validate compound and redeem behaviour" in {
    val startAtHeight = 5
    val action = pool01.deposit(input0)
    val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(0)).value
    val action1 = pool1.compound(sb1, epoch = 1)
    val (_, Right((pool2, bundle2, reward))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val action2 = pool2.redeem(bundle2)
    val (_, Right((pool3, rew))) = action2.run(RuntimeCtx.at(startAtHeight)).value

    val poolBox1 = pool1.toLedger[Ledger]
    val poolBox2 = pool2.toLedger[Ledger]
    val poolBox3 = pool3.toLedger[Ledger]

    val userBox0 = new UserBox(
      boxId("user"),
      0,
      tokens = Vector(
        tokenId("LM_Pool_NFT_ID") -> 0x7fffffffffffffffL,
        tokenId("LQ") -> 0,
        tokenId("X") -> reward.value,

      ),
      registers = Map(
      ))

    val bundleBox1 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      tokens = Vector(
        tokenId("LM_Pool_NFT_ID") -> 1,
        tokenId("VLQ") -> sb1.vLQ,
        tokenId("TMP") -> sb1.TMP
      ),
      registers = Map(
        4 -> SigmaProp("user"),
        5 -> tokenId("LM_Pool_NFT_ID"),
        6 -> tokenId("LM_Pool_NFT_ID"),
      ))

    val bundleBox2 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      tokens = Vector(
        tokenId("LM_Pool_NFT_ID") -> 1,
        tokenId("VLQ") -> bundle2.vLQ,
        tokenId("TMP") -> bundle2.TMP
      ),
      registers = Map(
        4 -> SigmaProp("user"),
        5 -> tokenId("LM_Pool_NFT_ID"),
        6 -> tokenId("LM_Pool_NFT_ID"),
      ))

    val bundleBox3 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      tokens = Vector(
        tokenId("LM_Pool_NFT_ID") -> 1,
        tokenId("VLQ") -> 0,
        tokenId("TMP") -> 0
      ),
      registers = Map(
        4 -> SigmaProp("user"),
        5 -> tokenId("LM_Pool_NFT_ID"),
        6 -> tokenId("LM_Pool_NFT_ID"),
      ))


    val (_, isValidCompound) = bundleBox1.validator.run(RuntimeCtx(startAtHeight, vars = Map(0 -> 1, 1 -> 2),
      inputs = List(poolBox1.setRegister(7, 1), bundleBox1),
      outputs = List(poolBox2.setRegister(7, 1), userBox0, bundleBox2))).value

    val (_, isValidRedeem) = bundleBox2.validator.run(RuntimeCtx(startAtHeight,
      inputs = List(poolBox2.setRegister(7, 1), bundleBox2, userBox0),
      outputs = List(poolBox3.setRegister(7, 1)))).value
    isValidCompound shouldBe true
    isValidRedeem shouldBe true

    val (_, isValidPool) = poolBox2.validator.run(RuntimeCtx(startAtHeight,
      outputs = List(poolBox3))).value

    isValidPool shouldBe true
    isValidCompound shouldBe true
    isValidRedeem shouldBe true

  }
}