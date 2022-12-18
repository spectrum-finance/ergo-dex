// Constants:
// ================================
// FeeDenom            : Int
// ExFeePerTokenNum    : Long
// ExFeePerTokenDenom  : Long
// MinQuoteAmount      : Long
// MaxExFee            : Long
// MaxMinerFee         : Long
// BaseAmount_x_FeeNum : BigInt
// SpectrumIsQuote     : Boolean
// SpectrumId          : Coll[Byte]
// QuoteId             : Coll[Byte]
// PoolNFT             : Coll[Byte]
// MinerPropBytes      : Coll[Byte]
// RedeemerPropBytes   : Coll[Byte]
// RefundProp          : ProveDlog
{
    val FeeDenom = 1000

    // Those constants are replaced when instantiating order:
    val ExFeePerTokenNum    = 1L
    val ExFeePerTokenDenom  = 10L
    val MinQuoteAmount      = 800L
    val MaxExFee            = 1400L
    val SpectrumIsQuote     = true // todo: make sure sigma produces same templates regardless of this const.

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
            val base       = SELF.tokens(0)
            val baseId     = base._1

            val poolNFT    = poolIn.tokens(0)._1
            val poolAssetX = poolIn.tokens(2)
            val poolAssetY = poolIn.tokens(3)

            val validPoolIn = poolNFT == PoolNFT

            val rewardBox   = OUTPUTS(1)
            val quoteAsset  = rewardBox.tokens(0)
            val quoteAmount =
                if (SpectrumIsQuote) {
                    val deltaQuote = quoteAsset._2.toBigInt - MaxExFee
                    deltaQuote.toBigInt * ExFeePerTokenDenom / (ExFeePerTokenDenom - ExFeePerTokenNum)
                } else {
                    quoteAsset._2.toBigInt
                }

            val valuePreserved = rewardBox.value >= SELF.value

            val fairExFee =
                if (SpectrumIsQuote) true
                else {
                    val exFee     = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
                    val remainder = MaxExFee - exFee
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
                    poolX * BaseAmount_x_FeeNum <= relaxedOutput * (poolY * FeeDenom + BaseAmount_x_FeeNum)
                else
                    poolY * BaseAmount_x_FeeNum <= relaxedOutput * (poolX * FeeDenom + BaseAmount_x_FeeNum)

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