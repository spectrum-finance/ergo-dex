package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.Helpers.tokenId
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{Box, RuntimeState}

final class SwapBuyBox[F[_] : RuntimeState](
                                             override val id: Coll[Byte],
                                             override val value: Long,
                                             override val creationHeight: Int,
                                             override val tokens: Vector[(Coll[Byte], Long)],
                                             override val registers: Map[Int, Any]
                                           ) extends Box[F] {
  override val validatorTag = "swap_buy_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Context (declarations here are only for simulations):

      val PoolNFT = tokenId("pool_NFT")
      val MinerPropBytes = tokenId("miner")
      val BaseAmount = SELF.tokens(0)._2
      val MaxMinerFee = 100L
      val RedeemerPropBytes = tokenId("redeemer")
      val SpectrumId = tokenId("y")


      // Contract
      val FeeDenom = 1000

      // Those constants are replaced when instantiating order:
      val FeeNum = 996
      val ExFeePerTokenNum = 1L
      val ExFeePerTokenDenom = 10L
      val MinQuoteAmount = 800L
      val MaxExFee = 1400L

      val poolIn = INPUTS(0)
      // Validations
      // 1.
      val validTrade =
      if (ctx.inputs.size >= 2 && poolIn.tokens.size == 3) { // replace with INPUTS.size
        val poolNFT = poolIn.tokens(0)._1 // first token id is NFT

        val poolReservesX = poolIn.value.toBigInt // nanoErgs is X asset amount
        val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount

        val validPoolIn = poolNFT == PoolNFT

        val rewardBox = OUTPUTS(1)

        val quoteAmount = rewardBox.value - SELF.value
        // 1.1.
        val fairExFee = {
          val exFee = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
          val remainder = MaxExFee - exFee
          if (remainder > 0) {
            val spectrumRem = rewardBox.tokens(0)
            spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
          } else {
            true
          }
        }
        val relaxedOutput = quoteAmount + 1 // handle rounding loss
        val base_x_feeNum = BaseAmount.toBigInt * FeeNum
        // 1.2.
        val fairPrice = poolReservesX * base_x_feeNum <= relaxedOutput * (poolReservesY * FeeDenom + base_x_feeNum)

        val minerOut = OUTPUTS(2)
        // 1.3.
        val validMinerFee = (minerOut.value <= MaxMinerFee) && (minerOut.propositionBytes == MinerPropBytes)
        // replace with
        // val validMinerFee = OUTPUTS.map { (o: Box) =>
        // if (o.propositionBytes == MinerPropBytes) o.value else 0L
        // }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

        validPoolIn &&
          rewardBox.propositionBytes == RedeemerPropBytes &&
          quoteAmount >= MinQuoteAmount &&
          fairExFee &&
          fairPrice &&
          validMinerFee
      } else false
      validTrade
    }
}