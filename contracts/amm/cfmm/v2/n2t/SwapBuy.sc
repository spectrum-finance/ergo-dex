// Constants:
// RedeemerPropBytes: Coll[Byte] - Bytes of the redeemer's proposition
// RefundProp: SigmaProp - Proposition that should be proved in order to refund the order
{   // Token -> ERG
    val FeeDenom = 1000
    val FeeNum   = 996
    val DexFeePerTokenNum   = 1L
    val DexFeePerTokenDenom = 10L

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
            val baseToken  = SELF.tokens(0) // token being sold
            val baseAmount = baseToken._2

            val poolNFT = poolIn.tokens(0)._1  // first token id is NFT

            val poolReservesX = poolIn.value.toBigInt   // nanoErgs is X asset amount
            val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount

            val validPoolIn = poolNFT == PoolNFT

            val rewardBox = OUTPUTS(1)

            val deltaNErgs    = rewardBox.value - SELF.value // this is quoteAmount - fee
            val quoteAmount   = deltaNErgs * DexFeePerTokenDenom / (DexFeePerTokenDenom - DexFeePerTokenNum)
            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val fairPrice     = poolReservesX * baseAmount * FeeNum <= relaxedOutput * (poolReservesY * FeeDenom + baseAmount * FeeNum)

            validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAmount >= MinQuoteAmount &&
            fairPrice
        } else false

    sigmaProp(RefundProp || validTrade)
}