package io.ergodex.core.lqmining.simple

import io.ergodex.core.Helpers.{boxId, bytes, hex}
import io.ergodex.core.LedgerPlatform
import io.ergodex.core.lqmining.simple.LMPool.{DefaultCreationHeight, _}
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
        bytes("lm_pool_id") -> BundleKeyTokenAmount
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
        bytes("vLQ")        -> expectedVLQAmount,
        bytes("TMP")        -> expectedTMPAmount,
        bytes("lm_pool_id") -> 1L
      ),
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> redeemerProp,
        7 -> bytes("LM_Pool_NFT_ID")
      )
    )

    (userBox, depositBox, bundleBox)
  }

  def getRedeemTxBoxes(
    redeemedVLQAmount: Long,
    expectedLQAmount: Long
  ): (UserBox[Ledger], RedeemBox[Ledger]) = {

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
        5  -> bytes("miner"),
        8  -> 100L,
        9  -> bytes("user"),
        10 -> bytes("LQ"),
        11 -> redeemedVLQAmount
      ),
      validatorBytes = "redeem"
    )

    (userBox, redeemBox)
  }

  def getCompoundTxBoxes(
    rewardAmount: Long,
    bundleVLQAmount: Long,
    bundleTMPAmountIn: Long,
    bundleTMPAmountOut: Long,
    validatorOutBytes: String = hex("staking_bundle")
  ): (UserBox[Ledger], UserBox[Ledger], StakingBundleBox[Ledger], StakingBundleBox[Ledger]) = {

    val C = Long.MaxValue - 1

    val userBoxReward = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("X") -> rewardAmount
      ),
      registers = Map.empty
    )

    val userBoxRedeem = new UserBox(
      boxId("user"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("bundle_key_id") -> C
      ),
      registers = Map.empty
    )

    val bundleBox1 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = Vector(
        bytes("VLQ")           -> bundleVLQAmount,
        bytes("TMP")           -> bundleTMPAmountIn,
        bytes("bundle_key_id") -> 1L
      ),
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> SigmaProp(bytes("user")),
        7 -> bytes("LM_Pool_NFT_ID")
      )
    )

    val tokensNew = {
      if (bundleTMPAmountOut == 0)
        Coll(
          bytes("VLQ")           -> bundleVLQAmount,
          bytes("bundle_key_id") -> 1L
        )
      else
        Coll(
          bytes("VLQ")           -> bundleVLQAmount,
          bytes("TMP")           -> bundleTMPAmountOut,
          bytes("bundle_key_id") -> 1L
        )
    }

    val bundleBox2 = new StakingBundleBox(
      boxId("bundle_box"),
      0,
      DefaultCreationHeight,
      tokens = tokensNew,
      registers = Map(
        4 -> bytes("yfToken"),
        5 -> bytes("yfTokenInfo"),
        6 -> SigmaProp(bytes("user")),
        7 -> bytes("LM_Pool_NFT_ID")
      ),
      validatorBytes = validatorOutBytes
    )
    (userBoxReward, userBoxRedeem, bundleBox1, bundleBox2)

  }
}
