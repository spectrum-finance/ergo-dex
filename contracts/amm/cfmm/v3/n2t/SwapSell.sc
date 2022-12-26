// Constants:
// ================================
// {1} -> MaxExFee[Long]
// {2} -> ExFeePerTokenDenom[Long]
// {3} -> BaseAmount[Long]
// {4} -> FeeNum[Int]
// {5} -> RefundProp[ProveDlog]
// {10} -> SpectrumIsQuote[Boolean]
// {13} -> PoolNFT[Coll[Byte]]
// {14} -> RedeemerPropBytes[Coll[Byte]]
// {15} -> QuoteId[Coll[Byte]]
// {16} -> MinQuoteAmount[Long]
// {19} -> ExFeePerTokenNum[Long]
// {22} -> SpectrumId[Coll[Byte]]
// {26} -> FeeDenom[Int]
// {27} -> MinerPropBytes[Coll[Byte]]
// {30} -> MaxMinerFee[Long]
// ================================
// ErgoTree: 19bd0520040005f01505c80105e01204c80f08cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a04040406040204000101059c0104000e2000000000000000000000000000000000000000000000000000000000000000000e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0e20020202020202020202020202020202020202020202020202020202020202020205c00c01010101052c06010004020e2001010101010101010101010101010101010101010101010101010101010101010101040406010104d00f0e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0500050005fe887a0100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c997e7209067e7202067e7203067e730b067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7202069d9c720a7e7313067e7203069591720b7314d801d60cb27207731500ed938c720c017316927e8c720c0206720b7317909c7e8cb2720573180002067e7204069c9a720a73199a9c7ec17201067e731a067e72040690b0ada5d9010b639593c2720b731bc1720b731c731dd9010b599a8c720b018c720b02731e731f
// ================================
// ErgoTreeTemplate: d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c997e7209067e7202067e7203067e730b067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7202069d9c720a7e7313067e7203069591720b7314d801d60cb27207731500ed938c720c017316927e8c720c0206720b7317909c7e8cb2720573180002067e7204069c9a720a73199a9c7ec17201067e731a067e72040690b0ada5d9010b639593c2720b731bc1720b731c731dd9010b599a8c720b018c720b02731e731f
{   // ERG -> Token
    val FeeDenom = 1000

    // Those constants are replaced when instantiating order:
    val FeeNum             = 996
    val ExFeePerTokenNum   = 22L
    val ExFeePerTokenDenom = 100L
    val MinQuoteAmount     = 800L
    val BaseAmount         = 1200L
    val MaxExFee           = 1400L
    val SpectrumIsQuote    = true // sigma produces same templates regardless of this const.

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
                if (SpectrumIsQuote) {
                    val deltaQuote = quoteAsset._2.toBigInt - MaxExFee
                    deltaQuote * ExFeePerTokenDenom / (ExFeePerTokenDenom - ExFeePerTokenNum)
                } else {
                    quoteAsset._2.toBigInt
                }

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

            val relaxedOutput = quoteAmount + 1 // handle rounding loss
            val base_x_feeNum = BaseAmount.toBigInt * FeeNum
            val fairPrice     = poolReservesY * base_x_feeNum <= relaxedOutput * (poolReservesX * FeeDenom + base_x_feeNum)

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