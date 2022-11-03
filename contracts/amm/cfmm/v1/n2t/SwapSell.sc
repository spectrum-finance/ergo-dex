{   // ERG -> Token
    val FeeDenom            = 1000
    val FeeNum              = 996
    val DexFeePerTokenNum   = 2L
    val DexFeePerTokenDenom = 10L
    val MinQuoteAmount      = 800L
    val BaseAmount          = 1200L

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
            val quoteAmount = quoteAsset._2.toBigInt

            val fairDexFee = rewardBox.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom - BaseAmount

            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val fairPrice     = poolReservesY * BaseAmount * FeeNum <= relaxedOutput * (poolReservesX * FeeDenom + BaseAmount * FeeNum)

            val validMinerFee = OUTPUTS.map { (o: Box) =>
                if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

            validPoolIn &&
            rewardBox.propositionBytes == Pk.propBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAmount >= MinQuoteAmount &&
            fairDexFee &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(Pk || validTrade)
}