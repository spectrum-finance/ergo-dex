package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.Helpers.tokenId
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{Box, RuntimeState}

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

      val SelfXAmount = SELF.tokens(0)._2
      val SelfYAmount = SELF.tokens(1)._2
      val PoolNFT = tokenId("pool_NFT")
      val MinerPropBytes = tokenId("miner")
      val MaxMinerFee = 100L
      val RedeemerPropBytes = tokenId("redeemer")

      // Contract
      val InitiallyLockedLP = 0x7fffffffffffffffL

      val poolIn = INPUTS(0)
      // Validations
      // 1.
      val validDeposit =
        if (ctx.inputs.size >= 2 && poolIn.tokens.size == 4) { // replace with INPUTS.size
          val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

          val poolLP = poolIn.tokens(1)
          val reservesX = poolIn.tokens(2)
          val reservesY = poolIn.tokens(3)

          val reservesXAmount = reservesX._2
          val reservesYAmount = reservesY._2

          val supplyLP = InitiallyLockedLP - poolLP._2

          val minByX = SelfXAmount.toBigInt * supplyLP / reservesXAmount
          val minByY = SelfYAmount.toBigInt * supplyLP / reservesYAmount

          val minimalReward = min(minByX, minByY)

          val rewardOut = OUTPUTS(1)
          val rewardLP = rewardOut.tokens(0)
          // 1.1.
          val validErgChange = rewardOut.value >= SELF.value
          // 1.2.
          val validTokenChange =
            if (minByX < minByY && rewardOut.tokens.size == 2) {
              val diff = minByY - minByX
              val excessY = diff * reservesYAmount / supplyLP

              val changeY = rewardOut.tokens(1)

              changeY._1 == reservesY._1 &&
                changeY._2 >= excessY
            } else if (minByX > minByY && rewardOut.tokens.size == 2) {
              val diff = minByX - minByY
              val excessX = diff * reservesXAmount / supplyLP

              val changeX = rewardOut.tokens(1)

              changeX._1 == reservesX._1 &&
                changeX._2 >= excessX
            } else if (minByX == minByY) {
              true
            } else {
              false
            }

          val minerOut = OUTPUTS(2)
          // 1.3.
          val validMinerFee = (minerOut.value <= MaxMinerFee) && (minerOut.propositionBytes == MinerPropBytes)
          // replace with
          // val validMinerFee = OUTPUTS.map { (o: Box) =>
          // if (o.propositionBytes == MinerPropBytes) o.value else 0L
          // }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee
          validPoolIn &&
            rewardOut.propositionBytes == RedeemerPropBytes &&
            validErgChange &&
            validTokenChange &&
            rewardLP._1 == poolLP._1 &&
            rewardLP._2 >= minimalReward &&
            validMinerFee
        } else false

      validDeposit
    }
}