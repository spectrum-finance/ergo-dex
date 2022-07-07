{   
    
    // Token -> ERG

    // RefundAddressPK: SigmaProp => Must be a P2PK address, otherwise refund is not possible.
    // ReceiverAddressPropositionBytes: Coll[Byte] => Can be proposition bytes of a P2PK or P2S address.

    val FeeDenom = 1000

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
            rewardBox.propositionBytes == ReceiverAddressPropositionBytes &&
            quoteAmount >= MinQuoteAmount &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(RefundAddressPK || validTrade)

}