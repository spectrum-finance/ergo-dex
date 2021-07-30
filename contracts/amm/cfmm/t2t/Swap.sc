{
    val FeeDenom = 1000

    val base       = SELF.tokens(0)
    val baseId     = base._1
    val baseAmount = base._2.toBigInt

    val poolInput  = INPUTS(0)
    val poolNFT    = poolInput.tokens(0)._1
    val poolAssetX = poolInput.tokens(2)
    val poolAssetY = poolInput.tokens(3)

    val validPoolInput = poolNFT == PoolNFT
    val noMoreInputs   = INPUTS.size == 2

    val rewardBox = OUTPUTS(1)

    val validTrade = {
        val quoteAsset    = rewardBox.tokens(0)
        val quoteAmount   = quoteAsset._2.toBigInt
        val fairDexFee    = rewardBox.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
        val relaxedOutput = quoteAmount + 1L // handle rounding loss
        val poolX         = poolAssetX._2.toBigInt
        val poolY         = poolAssetY._2.toBigInt
        val fairPrice     =
            if (poolAssetX._1 == QuoteId)
                poolX * baseAmount * FeeNum <= relaxedOutput * (poolY * FeeDenom + baseAmount * FeeNum)
            else
                poolY * baseAmount * FeeNum <= relaxedOutput * (poolX * FeeDenom + baseAmount * FeeNum)

        rewardBox.propositionBytes == Pk.propBytes &&
        quoteAsset._1 == QuoteId &&
        quoteAsset._2 >= MinQuoteAmount &&
        fairDexFee &&
        fairPrice
    }

    sigmaProp(Pk || (validPoolInput && noMoreInputs && validTrade))
}