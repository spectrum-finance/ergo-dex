{   // ERG -> Token
    val FeeDenom = 1000
    val FeeNum   = 996
    val DexFeePerTokenNum   = 1L
    val DexFeePerTokenDenom = 10L

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
            val poolNFT = poolIn.tokens(0)._1

            val poolY = poolIn.tokens(2)

            val poolReservesX = poolIn.value.toBigInt
            val poolReservesY = poolY._2.toBigInt
            val validPoolIn   = poolNFT == PoolNFT

            val rewardBox = OUTPUTS(1)

            val quoteAsset  = rewardBox.tokens(0)
            val quoteAmount = quoteAsset._2

            val fairDexFee = rewardBox.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom - BaseAmount

            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val fairPrice     = poolReservesY * BaseAmount * FeeNum <= relaxedOutput * (poolReservesX * FeeDenom + BaseAmount * FeeNum)

            validPoolIn &&
            rewardBox.propositionBytes == Pk.propBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAmount >= MinQuoteAmount &&
            fairDexFee &&
            fairPrice
        } else false

    sigmaProp(Pk || validTrade)
}