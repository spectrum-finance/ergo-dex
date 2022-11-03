{   // Token -> ERG
    val FeeDenom            = 1000
    val FeeNum              = 996
    val DexFeePerTokenNum   = 1L
    val DexFeePerTokenDenom = 10L
    val MinQuoteAmount      = 800L

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
            val baseAmount = SELF.tokens(0)._2

            val poolNFT = poolIn.tokens(0)._1  // first token id is NFT

            val poolReservesX = poolIn.value.toBigInt   // nanoErgs is X asset amount
            val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount

            val validPoolIn = poolNFT == PoolNFT

            val rewardBox = OUTPUTS(1)

            val deltaNErgs    = rewardBox.value - SELF.value // this is quoteAmount - fee
            val quoteAmount   = deltaNErgs.toBigInt * DexFeePerTokenDenom / (DexFeePerTokenDenom - DexFeePerTokenNum)
            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val fairPrice     = poolReservesX * baseAmount * FeeNum <= relaxedOutput * (poolReservesY * FeeDenom + baseAmount * FeeNum)

            val validMinerFee = OUTPUTS.map { (o: Box) =>
                if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

            validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAmount >= MinQuoteAmount &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(RefundProp || validTrade)
}