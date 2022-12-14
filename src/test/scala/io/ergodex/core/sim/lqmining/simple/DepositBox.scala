package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.Helpers.tokenId
import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{Box, RuntimeState}
import io.ergodex.core.syntax._

final class DepositBox[F[_] : RuntimeState](
                                             override val id: Coll[Byte],
                                             override val value: Long,
                                             override val creationHeight: Int,
                                             override val tokens: Vector[(Coll[Byte], Long)],
                                             override val registers: Map[Int, Any]
                                           ) extends Box[F] {
  override val validatorTag = "deposit_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Context (declarations here are only for simulations):
      val ExpectedNumEpochs = SELF.R5[Int].get
      val RedeemerProp = tokenId("user")
      val RefundPk = true
      val PoolId = tokenId("LM_Pool_NFT_ID")

      // ===== Contract Information ===== //
      // Name: Deposit
      // Description: Contract that validates user's deposit into the LM Pool.
      //
      // ===== Deposit Box ===== //
      // Tokens:
      //   0:
      //     _1: LQ Token ID  // identifier for the stake pool box.
      //     _2: Amount of LQ Tokens to deposit
      //
      // Validations:
      // 1. Assets are deposited into the correct LM Pool;
      // 2. Redeemer PubKey matches and correct Bundle Key token amount token is received;
      // 3. Bundle stores correct: Redeemer PubKey; vLQ token amount; TMP token amount; Bundle Key token amount.
      //
      // ===== Getting SELF data ===== //
      val deposit = SELF.tokens(0)

      // ===== Getting INPUTS data ===== //
      val poolIn = INPUTS(0)
      val bundleKeyId = poolIn.id

      // ===== Getting OUTPUTS data ===== //
      val redeemerOut = OUTPUTS(1) // 0 -> pool_out, 1 -> redeemer_out, 2 -> bundle_out
      val bundleOut = OUTPUTS(2)

      // ===== Calculations ===== //
      val expectedVLQ = deposit._2
      val expectedTMP = expectedVLQ * ExpectedNumEpochs

      // ===== Validating conditions ===== //
      // 1.
      val validPoolIn = poolIn.tokens(0)._1 == PoolId
      // 2.
      val validRedeemerOut =
        redeemerOut.propositionBytes == RedeemerProp &&
          (bundleKeyId, 0x7fffffffffffffffL - 1L) == redeemerOut.tokens(0)
      // 3.
      val validBundle =
        bundleOut.R4[Coll[Byte]].get == RedeemerProp &&
          bundleOut.R5[Coll[Byte]].get ==  PoolId &&
          (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(0) &&
          (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(1) &&
          (bundleKeyId, 1L) == bundleOut.tokens(2)

      validPoolIn && validRedeemerOut && validBundle
    }
}
