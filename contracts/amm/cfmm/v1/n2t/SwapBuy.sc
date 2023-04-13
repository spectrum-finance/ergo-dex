{ // ===== Contract Information ===== //
  // Name: SwapBuy
  // Description: Contract that validates user's Token -> ERG swap in the CFMM n2t Pool.
  //
  // ErgoTree: 1980031508d2040004040406040205140512040004000e20020202020202020202020202020202020202020202020202020202020202020205c00c04c80f060101040404d00f04c80f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d804d603db63087202d604b2a5730400d6059d9c7e99c17204c1a7067e7305067e730606d6068cb2db6308a773070002edededed938cb2720373080001730993c27204d072019272057e730a06909c9c7ec17202067e7206067e730b069c9a7205730c9a9c7e8cb27203730d0002067e730e067e9c72067e730f050690b0ada5d90107639593c272077310c1720773117312d90107599a8c7207018c72070273137314
  //
  // ErgoTreeTemplate: d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d804d603db63087202d604b2a5730400d6059d9c7e99c17204c1a7067e7305067e730606d6068cb2db6308a773070002edededed938cb2720373080001730993c27204d072019272057e730a06909c9c7ec17202067e7206067e730b069c9a7205730c9a9c7e8cb27203730d0002067e730e067e9c72067e730f050690b0ada5d90107639593c272077310c1720773117312d90107599a8c7207018c72070273137314

  val FeeDenom            = 1000
  val FeeNum              = 996
  val DexFeePerTokenNum   = 1L
  val DexFeePerTokenDenom = 10L
  val MinQuoteAmount      = 800L

  val poolIn = INPUTS(0)

  val validTrade =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
      val baseAmount = SELF.tokens(0)._2

      val poolNFT = poolIn.tokens(0)._1 // first token id is NFT

      val poolReservesX = poolIn.value.toBigInt // nanoErgs is X asset amount
      val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount

      val validPoolIn = poolNFT == PoolNFT

      val rewardBox = OUTPUTS(1)

      val deltaNErgs    = rewardBox.value - SELF.value // this is quoteAmount - fee
      val quoteAmount   = deltaNErgs.toBigInt * DexFeePerTokenDenom / (DexFeePerTokenDenom - DexFeePerTokenNum)
      val relaxedOutput = quoteAmount + 1 // handle rounding loss
      val fairPrice =
        poolReservesX * baseAmount * FeeNum <= relaxedOutput * (poolReservesY * FeeDenom + baseAmount * FeeNum)

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardBox.propositionBytes == Pk.propBytes &&
      quoteAmount >= MinQuoteAmount &&
      fairPrice &&
      validMinerFee

    } else false

  sigmaProp(Pk || validTrade)
}
