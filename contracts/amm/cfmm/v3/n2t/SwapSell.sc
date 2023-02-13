{ // ===== Contract Information ===== //
  // Name: SwapSell
  // Description: Contract that validates user's swap from ERG to token in the CFMM n2t Pool.
  //
  // Constants:
  //
  // {1} -> BaseAmount[Long]
  // {2} -> FeeNum[Int]
  // {3} -> RefundProp[ProveDlog]
  // {8} -> SpectrumIsQuote[Boolean]
  // {9} -> MaxExFee[Long]
  // {10} -> ExFeePerTokenDenom[Long]
  // {13} -> PoolNFT[Coll[Byte]]
  // {14} -> RedeemerPropBytes[Coll[Byte]]
  // {15} -> QuoteId[Coll[Byte]]
  // {16} -> MinQuoteAmount[Long]
  // {20} -> ExFeePerTokenNum[Long]
  // {24} -> SpectrumId[Coll[Byte]]
  // {28} -> FeeDenom[Int]
  // {29} -> MinerPropBytes[Coll[Byte]]
  // {32} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19e504220400060204b0060203e408cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040604020400010006020578060164059c0104000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e20040404040404040404040404040404040404040404040404040404040404040406020320010001010602057806011606016406010004020e20030303030303030303030303030303030303030303030303030303030303030301010404060101060203e80e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6029c73017302eb027303d195ed92b1a4730493b1db630872017305d806d603db63087201d604b2a5730600d605db63087204d606b27205730700d6078c720602d6089573089d9c997e7207067309730a7e730b067e720706edededededed938cb27203730c0001730d93c27204730e938c720601730f92720873109573117312d801d6099973139d9c720873147315959172097316d801d60ab27205731700ed938c720a017318927e8c720a020672097319909c7e8cb27203731a00020672029c9a7208731b9a9c7ec1720106731c720290b0ada5d90109639593c27209731dc17209731e731fd90109599a8c7209018c72090273207321
  //
  // ErgoTreeTemplate: d802d601b2a4730000d6029c73017302eb027303d195ed92b1a4730493b1db630872017305d806d603db63087201d604b2a5730600d605db63087204d606b27205730700d6078c720602d6089573089d9c997e7207067309730a7e730b067e720706edededededed938cb27203730c0001730d93c27204730e938c720601730f92720873109573117312d801d6099973139d9c720873147315959172097316d801d60ab27205731700ed938c720a017318927e8c720a020672097319909c7e8cb27203731a00020672029c9a7208731b9a9c7ec1720106731c720290b0ada5d90109639593c27209731dc17209731e731fd90109599a8c7209018c72090273207321

  val poolIn = INPUTS(0)

  // Validations
  // 1.
  val validTrade =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
      val poolNFT = poolIn.tokens(0)._1

      val poolY = poolIn.tokens(2)

      val poolReservesX = poolIn.value.toBigInt
      val poolReservesY = poolY._2.toBigInt
      val validPoolIn   = poolNFT == PoolNFT

      val rewardBox = OUTPUTS(1)

      val quoteAsset = rewardBox.tokens(0)
      val quoteAmount =
        if (SpectrumIsQuote) {
          val deltaQuote = quoteAsset._2.toBigInt - MaxExFee
          deltaQuote * ExFeePerTokenDenom / (ExFeePerTokenDenom - ExFeePerTokenNum)
        } else {
          quoteAsset._2.toBigInt
        }
      // 1.1.
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
      // 1.2.
      val fairPrice = poolReservesY * base_x_feeNum <= relaxedOutput * (poolReservesX * FeeDenom + base_x_feeNum)
      // 1.3.
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
