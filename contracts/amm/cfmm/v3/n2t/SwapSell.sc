{ // ===== Contract Information ===== //
  // Name: SwapSell
  // Description: Contract that validates user's swap from ERG to token in the CFMM n2t Pool.
  //
  // Constants:
  //
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
  // {23} -> SpectrumId[Coll[Byte]]
  // {27} -> FeeDenom[Int]
  // {28} -> MinerPropBytes[Coll[Byte]]
  // {31} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19fd0421040005f01505c80105e01204c80f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040204000101059c0104000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e20040404040404040404040404040404040404040404040404040404040404040405c00c01010101052c060100040404020e2003030303030303030303030303030303030303030303030303030303030303030101040406010104d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c997e7209067e7202067e7203067e730b067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7202069d9c720a7e7313067e72030695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320
  //
  // ErgoTreeTemplate: d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c997e7209067e7202067e7203067e730b067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7202069d9c720a7e7313067e72030695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320

  val baseAmount         = BaseAmount
  val feeNum             = FeeNum
  val feeDenom           = FeeDenom
  val maxExFee           = MaxExFee
  val exFeePerTokenDenom = ExFeePerTokenDenom
  val exFeePerTokenNum   = ExFeePerTokenNum
  val minQuoteAmount     = MinQuoteAmount

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
          val deltaQuote = quoteAsset._2.toBigInt - maxExFee
          deltaQuote * exFeePerTokenDenom / (exFeePerTokenDenom - exFeePerTokenNum)
        } else {
          quoteAsset._2.toBigInt
        }
      // 1.1.
      val fairExFee =
        if (SpectrumIsQuote) true
        else {
          val exFee     = quoteAmount * exFeePerTokenNum / exFeePerTokenDenom
          val remainder = maxExFee - exFee
          if (remainder > 0 && rewardBox.tokens.size >= 2) {
            val spectrumRem = rewardBox.tokens(1)
            spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
          } else {
            true
          }
        }

      val relaxedOutput = quoteAmount + 1L // handle rounding loss

      val base_x_feeNum = baseAmount.toBigInt * feeNum
      // 1.2.
      val fairPrice = poolReservesY * base_x_feeNum <= relaxedOutput * (poolReservesX * feeDenom + base_x_feeNum)
      // 1.3.
      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardBox.propositionBytes == RedeemerPropBytes &&
      quoteAsset._1 == QuoteId &&
      quoteAmount >= minQuoteAmount &&
      fairExFee &&
      fairPrice &&
      validMinerFee

    } else false

  sigmaProp(RefundProp || validTrade)
}
