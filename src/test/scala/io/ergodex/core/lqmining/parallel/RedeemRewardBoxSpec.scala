package io.ergodex.core.lqmining.parallel

import io.ergodex.core.lqmining.parallel.TxBoxes._
import io.ergodex.core.lqmining.parallel.generators.DepositGen
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class RedeemRewardBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  forAll(DepositGen) { rew =>
    val rewardMain = rew
    val rewardOpt  = rew / 2
    val testId     = Random.nextLong()

    it should s"validate redeem main reward behaviour$testId" in {
      val (bundleBox0, bundleBox1) = getCompoundTxBoxes(
        100L,
        rewardMain,
        rewardOpt,
        100L,
        1L,
        rewardOpt,
        100L,
        actionId = 1
      )

      val (redeemRewardsBox0, userBoxRedeem0) =
        getRedeemRewardsBoxes("X0", rewardMain, "X1", rewardMain, "user")

      val (_, isValidRedeemRewardBundle0) = bundleBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 3),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      val (_, isValidRedeemReward0) = redeemRewardsBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 3),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      isValidRedeemRewardBundle0 shouldBe true
      isValidRedeemReward0 shouldBe false
    }
    it should s"validate redeem optional reward behaviour$testId" in {
      val (bundleBox0, bundleBox1) = getCompoundTxBoxes(
        100L,
        rewardMain,
        rewardOpt,
        100L,
        rewardMain,
        1L,
        100L,
        actionId = 1
      )

      val (redeemRewardsBox0, userBoxRedeem0) =
        getRedeemRewardsBoxes("X1", rewardMain, "X1", rewardMain, "user")

      val (_, isValidRedeemRewardBundle0) = bundleBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 4),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      val (_, isValidRedeemReward0) = redeemRewardsBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 4),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      isValidRedeemRewardBundle0 shouldBe true
      isValidRedeemReward0 shouldBe true
    }

    it should s"validate illegal received reward main reward amount behaviour$testId" in {
      val (bundleBox0, bundleBox1) = getCompoundTxBoxes(
        100L,
        rewardMain,
        rewardOpt,
        100L,
        1L,
        rewardOpt,
        100L,
        actionId = 1
      )

      val (redeemRewardsBox0, userBoxRedeem0) =
        getRedeemRewardsBoxes("X0", rewardMain, "X0", rewardMain - 1L, "user")

      val (_, isValidRedeemRewardBundle0) = bundleBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 3),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      val (_, isValidRedeemReward0) = redeemRewardsBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 3),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      isValidRedeemRewardBundle0 shouldBe true
      isValidRedeemReward0 shouldBe false
    }

    it should s"validate illegal redeemer main reward redeem behaviour$testId" in {
      val (bundleBox0, bundleBox1) = getCompoundTxBoxes(
        100L,
        rewardMain,
        rewardOpt,
        100L,
        1L,
        rewardOpt,
        100L,
        actionId = 1
      )

      val (redeemRewardsBox0, userBoxRedeem0) =
        getRedeemRewardsBoxes("X0", rewardMain, "X0", rewardMain, "badman")

      val (_, isValidRedeemRewardBundle0) = bundleBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 3),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      val (_, isValidRedeemReward0) = redeemRewardsBox0.validator
        .run(
          RuntimeCtx(
            0,
            vars    = Map(0 -> 2, 1 -> 3),
            inputs  = List(bundleBox0, redeemRewardsBox0),
            outputs = List(bundleBox1, userBoxRedeem0)
          )
        )
        .value

      isValidRedeemRewardBundle0 shouldBe true
      isValidRedeemReward0 shouldBe false
    }
  }
}
