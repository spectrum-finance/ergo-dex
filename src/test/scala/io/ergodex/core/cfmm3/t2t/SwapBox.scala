package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.Helpers.tokenId
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{Box, RuntimeState}

final class SwapBox[F[_] : RuntimeState](
                                          override val id: Coll[Byte],
                                          override val value: Long,
                                          override val creationHeight: Int,
                                          override val tokens: Vector[(Coll[Byte], Long)],
                                          override val registers: Map[Int, Any]
                                        ) extends Box[F] {
  override val validatorTag = "swap_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Context (declarations here are only for simulations):

      val SpectrumIsQuote = getVar[Boolean](1).get

      val SpectrumId = tokenId("spf")
      val PoolNFT = tokenId("pool_NFT")
      val MinerPropBytes = tokenId("miner")
      val BaseAmount = SELF.tokens(0)._2
      val MaxMinerFee = 100L
      val MinQuoteAmount = 1L
      val QuoteId = tokenId(List("x", "y")(getVar[Int](0).get))
      val RedeemerPropBytes = tokenId("redeemer")

      // Contract
      val FeeDenom = 1000

      val FeeNum = 996
      val MaxExFee = 2000L
      val ExFeePerTokenNum = 3000L
      val ExFeePerTokenDenom = 40000L

      val poolIn = INPUTS(0)
      // Validations
      // 1.
      val validTrade =
        if (ctx.inputs.size >= 2 && poolIn.tokens.size == 4) {

          val poolNFT = poolIn.tokens(0)._1
          val poolAssetX = poolIn.tokens(2)
          val poolAssetY = poolIn.tokens(3)

          val validPoolIn = poolNFT == PoolNFT

          val rewardBox = OUTPUTS(1)
          val quoteAsset = rewardBox.tokens(0)
          val quoteAmount =
            if (SpectrumIsQuote) {
              val deltaQuote = quoteAsset._2.toBigInt - MaxExFee
              deltaQuote.toBigInt * ExFeePerTokenDenom / (ExFeePerTokenDenom - ExFeePerTokenNum)
            } else {
              quoteAsset._2.toBigInt
            }
          // 1.1.
          val valuePreserved = rewardBox.value >= SELF.value
          // 1.2.
          val fairExFee =
            if (SpectrumIsQuote) true
            else {
              val exFee = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
              val remainder = MaxExFee - exFee
              if (remainder > 0) {
                val spectrumRem = rewardBox.tokens(1)
                spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
              } else {
                true
              }
            }

          val relaxedOutput = quoteAmount + 1L // handle rounding loss
          val poolX = poolAssetX._2.toBigInt
          val poolY = poolAssetY._2.toBigInt
          val base_x_feeNum = BaseAmount.toBigInt * FeeNum
          // 1.3.
          val fairPrice =
            if (poolAssetX._1 == QuoteId) {
              poolX * base_x_feeNum <= relaxedOutput * (poolY * FeeDenom + base_x_feeNum)
            } else {
              poolY * base_x_feeNum <= relaxedOutput * (poolX * FeeDenom + base_x_feeNum)
            }

          val minerOut = OUTPUTS(2)
          // 1.4.
          val validMinerFee = (minerOut.value <= MaxMinerFee) && (minerOut.propositionBytes == MinerPropBytes)
          // replace with
          // val validMinerFee = OUTPUTS.map { (o: Box) =>
          // if (o.propositionBytes == MinerPropBytes) o.value else 0L
          // }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAsset._2 >= MinQuoteAmount &&
            valuePreserved &&
            fairExFee &&
            fairPrice &&
            validMinerFee
        } else false
      validTrade
    }
}