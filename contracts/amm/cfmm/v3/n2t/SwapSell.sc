{   // ERG -> Token
    val FeeDenom = 1000
    val FeeNum   = 996

    // Those constants are replaced when instantiating order:
    val ExFeePerTokenNum   = 2L
    val ExFeePerTokenDenom = 10L
    val MinQuoteAmount     = 800L
    val BaseAmount         = 1200L
    val ReservedExFee      = 1400L
    val SpectrumIsQuote    = true // todo: make sure sigma produces same templates regardless of this const.

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
            val poolNFT = poolIn.tokens(0)._1

            val poolY = poolIn.tokens(2)

            val poolReservesX = poolIn.value.toBigInt
            val poolReservesY = poolY._2.toBigInt
            val validPoolIn   = poolNFT == PoolNFT

            val rewardBox = OUTPUTS(1)

            val quoteAsset  = rewardBox.tokens(0)
            val quoteAmount =
                if (SpectrumIsQuote) FeeDenom * (quoteAsset._2.toBigInt - ReservedExFee) / (FeeDenom - FeeNum)
                else quoteAsset._2.toBigInt

            val fairExFee =
                if (SpectrumIsQuote) true
                else {
                    val exFee     = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
                    val remainder = ReservedExFee - exFee
                    if (remainder > 0) {
                        val spectrumRem = rewardBox.tokens(1)
                        spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
                    } else {
                        true
                    }
                }

            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val fairPrice     = poolReservesY * BaseAmount * FeeNum <= relaxedOutput * (poolReservesX * FeeDenom + BaseAmount * FeeNum)

            val validMinerFee = OUTPUTS.map { (o: Box) =>
                if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

            validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAmount >= MinQuoteAmount &&
            fairExFee &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(RefundProp || validTrade)
}