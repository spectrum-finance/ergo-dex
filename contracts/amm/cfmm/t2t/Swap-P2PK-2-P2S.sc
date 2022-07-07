{
    
    // Token -> Token

    // RefundAddressPK: SigmaProp => Must be a P2PK address, otherwise refund is not possible.
    // ReceiverAddressPropositionBytes: Coll[Byte] => Can be proposition bytes of a P2PK or P2S address.

    val FeeDenom = 1000

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size == 2 && poolIn.tokens.size == 4) {
            val base       = SELF.tokens(0)
            val baseId     = base._1
            val baseAmount = base._2.toBigInt

            val poolNFT    = poolIn.tokens(0)._1
            val poolAssetX = poolIn.tokens(2)
            val poolAssetY = poolIn.tokens(3)

            val validPoolIn = poolNFT == PoolNFT

            val rewardBox     = OUTPUTS(2)
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

            validPoolIn &&
            rewardBox.propositionBytes == ReceiverAddressPropositionBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAsset._2 >= MinQuoteAmount &&
            fairDexFee &&
            fairPrice
        } else false

    sigmaProp(RefundAddressPK || validTrade)

}