// Constants:
// ================================
// {1} -> BaseAmount[Long]
// {2} -> FeeNum[Int]
// {3} -> RefundProp[ProveDlog]
// {7} -> MaxExFee[Long]
// {8} -> ExFeePerTokenNum[Long]
// {9} -> ExFeePerTokenDenom[Long]
// {11} -> PoolNFT[Coll[Byte]]
// {12} -> RedeemerPropBytes[Coll[Byte]]
// {13} -> MinQuoteAmount[Long]
// {16} -> SpectrumId[Coll[Byte]]
// {20} -> FeeDenom[Int]
// {21} -> MinerPropBytes[Coll[Byte]]
// {24} -> MaxMinerFee[Long]
// ================================
// ErgoTree: 19c9041a040005e01204c80f08cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a04040406040205f015052c05c80104000e2000000000000000000000000000000000000000000000000000000000000000000e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa05c00c050004000e20010101010101010101010101010101010101010101010101010101010101010101010502040404d00f0e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0500050005fe887a0100d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d6069973079d9c720573087309ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310928c72070272067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319
// ================================
// ErgoTreeTemplate: d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d6069973079d9c720573087309ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310928c72070272067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319
{   // Token -> ERG
    val FeeDenom = 1000

    // Those constants are replaced when instantiating order:
    val FeeNum             = 996
    val ExFeePerTokenNum   = 22L
    val ExFeePerTokenDenom = 100L
    val MinQuoteAmount     = 800L
    val BaseAmount         = 1200L
    val MaxExFee           = 1400L

    val poolIn = INPUTS(0)

    val validTrade =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
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