package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.Helpers.tokenId
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{Box, RuntimeState}

final class RedeemBox[F[_] : RuntimeState](
                                            override val id: Coll[Byte],
                                            override val value: Long,
                                            override val creationHeight: Int,
                                            override val tokens: Vector[(Coll[Byte], Long)],
                                            override val registers: Map[Int, Any]
                                          ) extends Box[F] {
  override val validatorTag = "redeem_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Context (declarations here are only for simulations):
      val PoolNFT = tokenId("pool_NFT")
      val MinerPropBytes = tokenId("miner")
      val MaxMinerFee = 100L
      val RedeemerPropBytes = tokenId("redeemer")

      // Contract
      val InitiallyLockedLP = 0x7fffffffffffffffL

      val selfLP = SELF.tokens(0)

      val poolIn = INPUTS(0)
      // Validations
      // 1.
      val validRedeem =
        if (ctx.inputs.size >= 2 && poolIn.tokens.size == 4) { // replace with INPUTS.size
          val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

          val poolLP = poolIn.tokens(1)
          val reservesX = poolIn.tokens(2)
          val reservesY = poolIn.tokens(3)

          val supplyLP = InitiallyLockedLP - poolLP._2

          val selfLPAmount = selfLP._2.toBigInt
          val minReturnX = selfLPAmount * reservesX._2 / supplyLP
          val minReturnY = selfLPAmount * reservesY._2 / supplyLP

          val returnOut = OUTPUTS(1)

          val returnX = returnOut.tokens(0)
          val returnY = returnOut.tokens(1)

          val minerOut = OUTPUTS(2)
          // 1.1.
          val validMinerFee = (minerOut.value <= MaxMinerFee) && (minerOut.propositionBytes == MinerPropBytes)
          // replace with
          // val validMinerFee = OUTPUTS.map { (o: Box) =>
          // if (o.propositionBytes == MinerPropBytes) o.value else 0L
          // }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn &&
            returnOut.propositionBytes == RedeemerPropBytes &&
            returnX._1 == reservesX._1 &&
            returnY._1 == reservesY._1 &&
            returnX._2 >= minReturnX &&
            returnY._2 >= minReturnY &&
            validMinerFee
        } else false

      validRedeem
    }
}
