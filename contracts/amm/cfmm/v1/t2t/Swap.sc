{// ===== Contract Information ===== //
  // Name: Swap
  // Description: Contract that validates user's swap in the CFMM t2t Pool.
  //
  // ErgoTree: 1988041708d204000e20010101010101010101010101010101010101010101010101010101010101010104c80f04d00f040404080402040004040400040606010104000e20020202020202020202020202020202020202020202020202020202020202020205c00c050205140e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed92b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cedededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e72040690b0ada5d90110639593c272107312c1721073137314d90110599a8c7210018c72100273157316
  //
  // ErgoTreeTemplate: d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed92b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cedededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e72040690b0ada5d90110639593c272107312c1721073137314d90110599a8c7210018c72100273157316

  val FeeDenom            = 1000
  val FeeNum              = 996
  val DexFeePerTokenNum   = 1L
  val DexFeePerTokenDenom = 10L
  val MinQuoteAmount      = 800L

  val poolIn = INPUTS(0)

  val validTrade =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
      val base       = SELF.tokens(0)
      val baseAmount = base._2.toBigInt

      val poolNFT    = poolIn.tokens(0)._1
      val poolAssetX = poolIn.tokens(2)
      val poolAssetY = poolIn.tokens(3)

      val validPoolIn = poolNFT == PoolNFT

      val rewardBox     = OUTPUTS(1)
      val quoteAsset    = rewardBox.tokens(0)
      val quoteAmount   = quoteAsset._2.toBigInt
      val dexFee        = quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom
      val fairDexFee    = rewardBox.value >= SELF.value - dexFee
      val relaxedOutput = quoteAmount + 1L // handle rounding loss
      val poolX         = poolAssetX._2.toBigInt
      val poolY         = poolAssetY._2.toBigInt

      val fairPrice =
        if (poolAssetX._1 == QuoteId)
          poolX * baseAmount * FeeNum <= relaxedOutput * (poolY * FeeDenom + baseAmount * FeeNum)
        else
          poolY * baseAmount * FeeNum <= relaxedOutput * (poolX * FeeDenom + baseAmount * FeeNum)

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardBox.propositionBytes == Pk.propBytes &&
      quoteAsset._1 == QuoteId &&
      quoteAsset._2 >= MinQuoteAmount &&
      fairDexFee &&
      fairPrice &&
      validMinerFee

    } else false

  sigmaProp(Pk || validTrade)
}
