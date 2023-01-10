{ // ===== Contract Information ===== //
  // Name: Swap
  // Description: Contract that validates user's swap from token to token in the CFMM t2t Pool.
  //
  // Constants:
  //
  // {1} -> QuoteId[Coll[Byte]]
  // {2} -> RefundProp[ProveDlog]
  // {7} -> SpectrumIsQuote[Boolean]
  // {8} -> MaxExFee[Long]
  // {12} -> BaseAmount[Long]
  // {13} -> FeeNum[Int]
  // {17} -> PoolNFT[Coll[Byte]]
  // {18} -> RedeemerPropBytes[Coll[Byte]]
  // {19} -> MinQuoteAmount[Long]
  // {23} -> ExFeePerTokenNum[Long]
  // {24} -> ExFeePerTokenDenom[Long]
  // {27} -> SpectrumId[Coll[Byte]]
  // {29} -> FeeDenom[Int]
  // {31} -> MinerPropBytes[Coll[Byte]]
  // {34} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19a3052404000e20040404040404040404040404040404040404040404040404040404040404040408cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040804020400010006020578060164059c010404060204b0060203e4040606010104000e2002020202020202020202020202020202020202020202020202020202020202020e20010101010101010101010101010101010101010101010101010101010101010105c00c010001010602057806011606016406010004020e2003030303030303030303030303030303030303030303030303030303030303030101060203e8060203e80e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605db63087204d606b27205730600d6078c720602d6089573079d9c997e720706730873097e730a067e720706d609b27203730b00d60a7e8c72090206d60b9c730c730dd60c7e8cb27203730e000206d60d9a7208730fededededededed938cb2720373100001731193c272047312938c7206017202927207731392c17204c1a79573147315d801d60e9973169d9c7208731773189591720e7319d801d60fb27205731a00ed938c720f01731b927e8c720f0206720e731c95938c7209017202909c720a720b9c720d9a9c720c731d720b909c720c720b9c720d9a9c720a731e720b90b0ada5d9010e639593c2720e731fc1720e73207321d9010e599a8c720e018c720e0273227323
  //
  // ErgoTreeTemplate: d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605db63087204d606b27205730600d6078c720602d6089573079d9c997e720706730873097e730a067e720706d609b27203730b00d60a7e8c72090206d60b9c730c730dd60c7e8cb27203730e000206d60d9a7208730fededededededed938cb2720373100001731193c272047312938c7206017202927207731392c17204c1a79573147315d801d60e9973169d9c7208731773189591720e7319d801d60fb27205731a00ed938c720f01731b927e8c720f0206720e731c95938c7209017202909c720a720b9c720d9a9c720c731d720b909c720c720b9c720d9a9c720a731e720b90b0ada5d9010e639593c2720e731fc1720e73207321d9010e599a8c720e018c720e0273227323

  val poolIn = INPUTS(0)

  // Validations
  // 1.
  val validTrade =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {

      val poolNFT    = poolIn.tokens(0)._1
      val poolAssetX = poolIn.tokens(2)
      val poolAssetY = poolIn.tokens(3)

      val validPoolIn = poolNFT == PoolNFT

      val rewardBox      = OUTPUTS(1)
      val quoteAsset     = rewardBox.tokens(0)
      val quoteAmount    =
        if (SpectrumIsQuote) {
          val deltaQuote = quoteAsset._2.toBigInt - MaxExFee
          deltaQuote.toBigInt * ExFeePerTokenDenom / (ExFeePerTokenDenom - ExFeePerTokenNum)
        } else {
          quoteAsset._2.toBigInt
        }
      // 1.1.
      val valuePreserved = rewardBox.value >= SELF.value
      // 1.2.
      val fairExFee      =
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
      // 1.3.
      val fairPrice     =
        if (poolAssetX._1 == QuoteId) {
          poolX * base_x_feeNum <= relaxedOutput * (poolY * FeeDenom + base_x_feeNum)
        } else {
          poolY * base_x_feeNum <= relaxedOutput * (poolX * FeeDenom + base_x_feeNum)
        }
      // 1.4.
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
