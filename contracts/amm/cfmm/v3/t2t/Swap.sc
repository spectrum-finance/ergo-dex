{
    val FeeDenom = 1000
    val FeeNum   = 996

    // Those constants are replaced when instantiating order:
    val DexFeePerTokenNum   = 1L
    val DexFeePerTokenDenom = 10L
    val MinQuoteAmount      = 800L
    val ReservedExFee       = 1400L
    val SpectrumIsQuote     = true // todo: make sure sigma produces same templates regardless of this const.

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size == 2 && poolIn.tokens.size == 4) {
            val base       = SELF.tokens(0)
            val baseId     = base._1
            val baseAmount = (if (baseId != SpectrumId) base._2 else base._2 - ReservedExFee).toBigInt

            val poolNFT    = poolIn.tokens(0)._1
            val poolAssetX = poolIn.tokens(2)
            val poolAssetY = poolIn.tokens(3)

            val validPoolIn = poolNFT == PoolNFT

            val rewardBox   = OUTPUTS(1)
            val quoteAsset  = rewardBox.tokens(0)
            val quoteAmount =
                if (SpectrumIsQuote) FeeDenom * (quoteAsset._2.toBigInt - ReservedExFee) / (FeeDenom - FeeNum)
                else quoteAsset._2.toBigInt

            val valuePreserved = rewardBox.value >= SELF.value

            val fairExFee =
                if (SpectrumIsQuote) true
                else {
                    val exFee     = quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
                    val remainder = ReservedExFee - exFee
                    if (remainder > 0) {
                        val spectrumRem = rewardBox.tokens(1)
                        spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
                    } else {
                        true
                    }
                }

            val relaxedOutput = quoteAmount + 1L // handle rounding loss
            val poolX         = poolAssetX._2.toBigInt
            val poolY         = poolAssetY._2.toBigInt
            val fairPrice     =
                if (poolAssetX._1 == QuoteId)
                    poolX * baseAmount * FeeNum <= relaxedOutput * (poolY * FeeDenom + baseAmount * FeeNum)
                else
                    poolY * baseAmount * FeeNum <= relaxedOutput * (poolX * FeeDenom + baseAmount * FeeNum)

            val validMinerFee = OUTPUTS.map { (o: Box) =>
                if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

            validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAsset._2 >= MinQuoteAmount &&
            fairExFee &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(RefundProp || validTrade)
}