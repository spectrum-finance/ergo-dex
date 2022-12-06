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
  val maxRoundingError = 1000L
  val redeemDelta = 10
  val epochReg = 8
  val pool01: LMPool[Ledger] =
    LMPool.init(epochLen = 3, epochNum = 4, programStart = 2, redeemDelta, programBudget = 900000000L * maxRoundingError, maxRoundingError)

  val input0: AssetInput[LQ] = AssetInput(1000 * maxRoundingError)

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
        tokenId("X") -> reward.value,

      ),
      registers = Map(
      ))

    val bundleBox1 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      tokens = Vector(
        tokenId("VLQ") -> sb1.vLQ,
        tokenId("TMP") -> sb1.TMP
      ),
      registers = Map(
        4 -> tokenId("user"),
        5 -> tokenId("lm_pool_id"),
        6 -> tokenId("LM_Pool_NFT_ID"),
      ))

    val bundleBox2 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      tokens = Vector(
        tokenId("VLQ") -> bundle2.vLQ,
        tokenId("TMP") -> bundle2.TMP
      ),
      registers = Map(
        4 -> tokenId("user"),
        5 -> tokenId("lm_pool_id"),
        6 -> tokenId("LM_Pool_NFT_ID"),
      ))


    val (_, isValidCompound) = bundleBox1.validator.run(RuntimeCtx(startAtHeight, vars = Map(0 -> 1, 1 -> 2),
      inputs = List(poolBox1.setRegister(epochReg, 1), bundleBox1),
      outputs = List(poolBox2.setRegister(epochReg, 1), userBox0, bundleBox2))).value

    val (_, isValidPool) = poolBox2.validator.run(RuntimeCtx(startAtHeight,
      outputs = List(poolBox3))).value

    isValidPool shouldBe true
    isValidCompound shouldBe true
  }
}