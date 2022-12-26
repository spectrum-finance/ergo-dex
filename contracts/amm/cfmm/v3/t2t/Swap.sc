// Constants:
// ================================
// {1} -> QuoteId[Coll[Byte]]
// {2} -> MaxExFee[Long]
// {3} -> ExFeePerTokenDenom[Long]
// {4} -> BaseAmount[Long]
// {5} -> FeeNum[Int]
// {6} -> FeeDenom[Int]
// {7} -> RefundProp[ProveDlog]
// {12} -> SpectrumIsQuote[Boolean]
// {18} -> PoolNFT[Coll[Byte]]
// {19} -> RedeemerPropBytes[Coll[Byte]]
// {20} -> MinQuoteAmount[Long]
// {23} -> ExFeePerTokenNum[Long]
// {26} -> SpectrumId[Coll[Byte]]
// {28} -> MinerPropBytes[Coll[Byte]]
// {31} -> MaxMinerFee[Long]
// ================================
// ErgoTree: 1983062104000e20020202020202020202020202020202020202020202020202020202020202020205a01f0580f10405d00f04c80f04d00f08cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a040404080402040001010590c2040404040606010104000e2000000000000000000000000000000000000000000000000000000000000000000e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa05f8060101010105f02e06010004020e20010101010101010101010101010101010101010101010101010101010101010101010e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0500050005fe887a0100d806d601b2a4730000d6027301d6037302d6047303d6059c73047e730505d6067306eb027307d195ed92b1a4730893b1db630872017309d80ad607db63087201d608b2a5730a00d609db63087208d60ab27209730b00d60b8c720a02d60c95730c9d9c997e720b067e7203067e7204067e730d067e720b06d60db27207730e00d60e7e8c720d0206d60f7e8cb27207730f000206d6109a720c7310ededededededed938cb2720773110001731293c272087313938c720a01720292720b731492c17208c1a79573157316d801d611997e7203069d9c720c7e7317067e720406959172117318d801d612b27209731900ed938c721201731a927e8c721202067211731b95938c720d017202909c720e7e7205069c72109a9c720f7e7206067e720506909c720f7e7205069c72109a9c720e7e7206067e72050690b0ada5d90111639593c27211731cc17211731d731ed90111599a8c7211018c721102731f7320
// ================================
// ErgoTreeTemplate: d806d601b2a4730000d6027301d6037302d6047303d6059c73047e730505d6067306eb027307d195ed92b1a4730893b1db630872017309d80ad607db63087201d608b2a5730a00d609db63087208d60ab27209730b00d60b8c720a02d60c95730c9d9c997e720b067e7203067e7204067e730d067e720b06d60db27207730e00d60e7e8c720d0206d60f7e8cb27207730f000206d6109a720c7310ededededededed938cb2720773110001731293c272087313938c720a01720292720b731492c17208c1a79573157316d801d611997e7203069d9c720c7e7317067e720406959172117318d801d612b27209731900ed938c721201731a927e8c721202067211731b95938c720d017202909c720e7e7205069c72109a9c720f7e7206067e720506909c720f7e7205069c72109a9c720e7e7206067e72050690b0ada5d90111639593c27211731cc17211731d731ed90111599a8c7211018c721102731f7320

{
    val FeeDenom = 1000

    val FeeNum             = 996
    val BaseAmount         = 1000L
    val MaxExFee           = 2000L
    val ExFeePerTokenNum   = 3000L
    val ExFeePerTokenDenom = 40000L

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {

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
            val base_x_feeNum = BaseAmount.toBigInt * FeeNum
            val fairPrice     =
                if (poolAssetX._1 == QuoteId) {
                    poolX * base_x_feeNum <= relaxedOutput * (poolY * FeeDenom + base_x_feeNum)
                } else {
                    poolY * base_x_feeNum <= relaxedOutput * (poolX * FeeDenom + base_x_feeNum)
                }

            val validMinerFee = OUTPUTS.map { (o: Box) =>
                if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

            validPoolIn &&
            rewardBox.propositionBytes == RedeemerPropBytes &&
            quoteAsset._1 == QuoteId &&
            quoteAsset._2 >= MinQuoteAmount &&
            valuePreserved &&
            fairExFee &&
            fairPrice &&
            validMinerFee
        } else false

    sigmaProp(RefundProp || validTrade)
}
