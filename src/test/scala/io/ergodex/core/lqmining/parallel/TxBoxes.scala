package io.ergodex.core.lqmining.parallel

import io.ergodex.core.Helpers.{boxId, bytes, hex}
import io.ergodex.core.LedgerPlatform
import io.ergodex.core.lqmining.parallel.LMPool._
import io.ergodex.core.syntax.{blake2b256, Coll, SigmaProp}
import org.scalatest.flatspec.AnyFlatSpec

object TxBoxes extends AnyFlatSpec with LedgerPlatform {

  def getDepositTxBoxes(
    depositedLQAmount: Long,
    expectedNumEpochs: Int,
    expectedVLQAmount: Long,
    expectedTMPAmount: Long,
    bundleValidatorBytesTag: String = "staking_bundle",
    redeemerProp: SigmaProp         = SigmaProp(bytes("user"))
  ): (UserBox[Ledger], DepositBox[Ledger], StakingBundleBox[Ledger]) = {

    val userBox = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("bundle_key_id") -> BundleKeyTokenAmount
      ),
      registers = Map.empty
    )
    val depositBox = new DepositBox(
      boxId("deposit_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("LQ") -> depositedLQAmount
      ),
      registers = Map.empty,
      constants = Map(
        1  -> bytes("LM_Pool_NFT_ID"),
        2  -> bytes("user"),
        3  -> false,
        12 -> blake2b256(bytes(bundleValidatorBytesTag)),
        16 -> expectedNumEpochs,
        20 -> bytes("miner"),
        23 -> 100L
      ),
      validatorBytes = hex("deposit_order")
    )

    val bundleBox = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("VLQ")           -> expectedVLQAmount,
        bytes("TMP")           -> expectedTMPAmount,
        bytes("bundle_key_id") -> 1L
      ),
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> bytes("LM_Pool_NFT_ID")
      ),
      constants = Map(
        1 -> 0
      )
    )

    (userBox, depositBox, bundleBox)
  }

  def getRedeemTxBoxes(
    bundleTmp: Long,
    bundleX0: Long,
    bundleX1: Long,
    redeemedVLQAmount: Long,
    expectedLQAmount: Long
  ): (UserBox[Ledger], RedeemBox[Ledger], StakingBundleBox[Ledger]) = {

    val userBox = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("LQ") -> expectedLQAmount
      ),
      registers = Map.empty
    )

    val redeemBox = new RedeemBox(
      boxId("redeem_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("bundle_key_id") -> BundleKeyTokenAmount
      ),
      registers = Map.empty,
      constants = Map(
        0  -> false,
        5  -> bytes("user"),
        6  -> bytes("LQ"),
        7  -> redeemedVLQAmount,
        9  -> bytes("miner"),
        12 -> 100L
      ),
      validatorBytes = hex("redeem")
    )
    val tokens = {
      if (bundleTmp == 0) {
        Coll(
          bytes("VLQ")           -> redeemedVLQAmount,
          bytes("TMP")           -> bundleTmp,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> bundleX0,
          bytes("X1")            -> bundleX1
        )
      } else {
        Coll(
          bytes("VLQ")           -> redeemedVLQAmount,
          bytes("TMP")           -> bundleTmp,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> bundleX0,
          bytes("X1")            -> bundleX0
        )
      }
    }
    val bundleBox1 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = tokens,
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> bytes("LM_Pool_NFT_ID")
      ),
      constants = Map(1 -> 1)
    )

    (userBox, redeemBox, bundleBox1)
  }

  def getCompoundTxBoxes(
    bundleVLQAmountIn: Long,
    rewardMainAmountIn: Long,
    rewardOptAmountIn: Long,
    bundleTMPAmountIn: Long,
    rewardMainAmountOut: Long,
    rewardOptAmountOut: Long,
    bundleTMPAmountOut: Long,
    validatorOutBytes: String = hex("staking_bundle"),
    actionId: Int             = 0,
    actionIdOut: Int          = 0
  ): (StakingBundleBox[Ledger], StakingBundleBox[Ledger]) = {

    val tokens0 = {
      if (rewardMainAmountIn == 0 && rewardOptAmountIn == 0) {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("TMP")           -> bundleTMPAmountIn,
          bytes("bundle_key_id") -> 1L
        )
      } else if (rewardMainAmountIn != 0 && rewardOptAmountIn == 0) {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("TMP")           -> bundleTMPAmountIn,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountIn
        )
      } else if (bundleTMPAmountIn == 0 && rewardMainAmountIn != 0 && rewardOptAmountIn != 0) {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountIn,
          bytes("X1")            -> rewardOptAmountIn
        )
      } else {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("TMP")           -> bundleTMPAmountIn,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountIn,
          bytes("X1")            -> rewardOptAmountIn
        )
      }
    }

    val bundleBox1 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = tokens0,
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> bytes("LM_Pool_NFT_ID")
      ),
      constants = Map(1 -> actionId)
    )

    val tokens1 = {

      if (bundleTMPAmountOut != 0 && rewardMainAmountOut != 0 && rewardOptAmountOut != 0) {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("TMP")           -> bundleTMPAmountOut,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountOut,
          bytes("X1")            -> rewardOptAmountOut
        )
      } else if (bundleTMPAmountOut != 0 && rewardMainAmountOut != 0 && rewardOptAmountOut == 0) {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("TMP")           -> bundleTMPAmountOut,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountOut
        )
      } else if (bundleTMPAmountOut == 0 && rewardMainAmountOut != 0 && rewardOptAmountOut != 0) {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountOut,
          bytes("X1")            -> rewardOptAmountOut
        )
      } else {
        Coll(
          bytes("VLQ")           -> bundleVLQAmountIn,
          bytes("bundle_key_id") -> 1L,
          bytes("X0")            -> rewardMainAmountOut
        )
      }
    }

    val bundleBox2 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = tokens1,
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> bytes("LM_Pool_NFT_ID")
      ),
      constants      = Map(1 -> actionIdOut),
      validatorBytes = validatorOutBytes
    )

    (bundleBox1, bundleBox2)

  }

  def getDepositBudgetBox(
    depositedBudgetToken: String,
    depositedBudgetTokenAmount: Long,
    owner: String,
    budgetTokenInd: Int
  ): DepositBudgetBox[Ledger] =
    new DepositBudgetBox(
      boxId("deposit_budget"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes(depositedBudgetToken) -> depositedBudgetTokenAmount
      ),
      registers      = Map.empty,
      validatorBytes = hex("depositBudget"),
      constants = Map(
        1  -> false,
        5  -> bytes("LM_Pool_NFT_ID"),
        6  -> budgetTokenInd,
        8  -> bytes(owner),
        12 -> bytes("miner"),
        15 -> 100L
      )
    )

  def getRedeemBudgetBoxes(
    expectedBudgetToken: String,
    expectedBudgetTokenAmount: Long,
    recBudgetAmount: Long,
    redeemerValidator: String,
    owner: String
  ): (RedeemBudgetBox[Ledger], UserBox[Ledger]) = {
    val RedeemBudgetBox = new RedeemBudgetBox(
      boxId("redeem_budget"),
      0,
      DefaultCreationHeight,
      registers      = Map.empty,
      tokens         = Vector(),
      validatorBytes = hex("redeemBudget"),
      constants = Map(
        0  -> false,
        5  -> bytes(owner),
        6  -> bytes(expectedBudgetToken),
        7  -> expectedBudgetTokenAmount,
        9  -> bytes("miner"),
        12 -> 100L
      )
    )

    val userBoxRedeem = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes(expectedBudgetToken) -> recBudgetAmount
      ),
      registers      = Map.empty,
      validatorBytes = hex(redeemerValidator)
    )
    (RedeemBudgetBox, userBoxRedeem)
  }

  def getRedeemRewardsBoxes(
    expectedRewardToken: String,
    expectedRewardTokenAmount: Long,
    recievedRewardToken: String,
    recievedRewardTokenAmount: Long,
    redeemer: String
  ): (RedeemRewardsBox[Ledger], UserBox[Ledger]) = {
    val redeemRewardsBox = new RedeemRewardsBox(
      boxId("redeem_budget"),
      0,
      DefaultCreationHeight,
      registers = Map.empty,
      tokens = Vector(
        bytes("bundle_key_id") -> BundleKeyTokenAmount
      ),
      validatorBytes = hex("redeemRewards"),
      constants = Map(
        0  -> false,
        3  -> bytes("user"),
        6  -> bytes(expectedRewardToken),
        7  -> expectedRewardTokenAmount,
        9  -> bytes("miner"),
        12 -> 100L
      )
    )
    val userBoxRedeem = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("bundle_key_id")     -> BundleKeyTokenAmount,
        bytes(recievedRewardToken) -> recievedRewardTokenAmount
      ),
      registers      = Map.empty,
      validatorBytes = hex(redeemer)
    )
    (redeemRewardsBox, userBoxRedeem)
  }
}
