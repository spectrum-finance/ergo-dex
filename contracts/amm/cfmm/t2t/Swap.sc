{
    val FeeDenom = 1000

    val base       = SELF.tokens(0)
    val baseId     = base._1
    val baseAmount = base._2

    val poolInput  = INPUTS(0)
    val poolNFT    = poolInput.tokens(0)._1
    val poolAssetX = poolInput.tokens(2)
    val poolAssetY = poolInput.tokens(3)

    val validPoolInput =
        poolNFT == PoolNFT &&
        (poolAssetX._1 == QuoteId || poolAssetY._1 == QuoteId) &&
        (poolAssetX._1 == baseId  || poolAssetY._1 == baseId)

    val validTrade =
        OUTPUTS.exists { (box: Box) =>
            val quoteAsset    = box.tokens(0)
            val quoteAmount   = quoteAsset._2
            val fairDexFee    = box.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val fairPrice     =
                if (poolAssetX._1 == QuoteId)
                    poolAssetX._2.toBigInt * baseAmount * FeeNum <= relaxedOutput * (poolAssetY._2.toBigInt * FeeDenom + baseAmount * FeeNum)
                else
                    poolAssetY._2.toBigInt * baseAmount * FeeNum <= relaxedOutput * (poolAssetX._2.toBigInt * FeeDenom + baseAmount * FeeNum)

            box.propositionBytes == Pk.propBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAsset._2 >= MinQuoteAmount &&
            fairDexFee &&
            fairPrice
        }

    sigmaProp(Pk || (validPoolInput && validTrade))
}