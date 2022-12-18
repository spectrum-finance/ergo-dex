// Constants:
// ================================
// FeeDenom            : Int
// FeeNum              : Int
// BaseAmount          : Long
// ExFeePerTokenNum    : Long
// ExFeePerTokenDenom  : Long
// MinQuoteAmount      : Long
// MaxExFee            : Long
// MaxMinerFee         : Long
// SpectrumId          : Coll[Byte]
// PoolNFT             : Coll[Byte]
// MinerPropBytes      : Coll[Byte]
// RedeemerPropBytes   : Coll[Byte]
// RefundProp          : ProveDlog
{   // Token -> ERG
    val FeeDenom = 1000
    val FeeNum   = 996

    // Those constants are replaced when instantiating order:
    val ExFeePerTokenNum   = 1L
    val ExFeePerTokenDenom = 10L
    val MinQuoteAmount     = 800L
    val MaxExFee           = 1400L

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
            val base       = SELF.tokens(0)
            val baseId     = base._1

            val poolNFT = poolIn.tokens(0)._1  // first token id is NFT

            val poolReservesX = poolIn.value.toBigInt   // nanoErgs is X asset amount
            val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount

            val validPoolIn = poolNFT == PoolNFT

            val rewardBox = OUTPUTS(1)

            val quoteAmount   = rewardBox.value - SELF.value
            val fairExFee     = {
                val exFee     = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
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
            val fairPrice     = poolReservesX * base_x_feeNum <= relaxedOutput * (poolReservesY * FeeDenom + base_x_feeNum)

            val validMinerFee = OUTPUTS.map { (o: Box) =>
                if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

            validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAmount >= MinQuoteAmount &&
            fairExFee &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(RefundProp || validTrade)
}