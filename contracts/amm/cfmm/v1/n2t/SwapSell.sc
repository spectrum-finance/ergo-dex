{ // ===== Contract Information ===== //
  // Name: SwapSell
  // Description: Contract that validates user's ERG -> Token swap in the CFMM n2t Pool.
  //
  // ErgoTree: 19c4031808d2040005e012040404060402040004000e2002020202020202020202020202020202020202020202020202020202020202020e20010101010101010101010101010101010101010101010101010101010101010105c00c05040514040404c80f06010104d00f05e01204c80f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d803d6017300d602b2a4730100d6037302eb027201d195ed92b1a4730393b1db630872027304d804d604db63087202d605b2a5730500d606b2db63087205730600d6077e8c72060206edededededed938cb2720473070001730893c27205d07201938c72060173099272077e730a06927ec172050699997ec1a7069d9c72077e730b067e730c067e720306909c9c7e8cb27204730d0002067e7203067e730e069c9a7207730f9a9c7ec17202067e7310067e9c73117e7312050690b0ada5d90108639593c272087313c1720873147315d90108599a8c7208018c72080273167317
  //
  // ErgoTreeTemplate: d803d6017300d602b2a4730100d6037302eb027201d195ed92b1a4730393b1db630872027304d804d604db63087202d605b2a5730500d606b2db63087205730600d6077e8c72060206edededededed938cb2720473070001730893c27205d07201938c72060173099272077e730a06927ec172050699997ec1a7069d9c72077e730b067e730c067e720306909c9c7e8cb27204730d0002067e7203067e730e069c9a7207730f9a9c7ec17202067e7310067e9c73117e7312050690b0ada5d90108639593c272087313c1720873147315d90108599a8c7208018c72080273167317

  val FeeDenom            = 1000
  val FeeNum              = 996
  val DexFeePerTokenNum   = 2L
  val DexFeePerTokenDenom = 10L
  val MinQuoteAmount      = 800L
  val BaseAmount          = 1200L

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
      val quoteAmount = quoteAsset._2.toBigInt

      val fairDexFee =
        rewardBox.value >= SELF.value - quoteAmount * DexFeePerTokenNum / DexFeePerTokenDenom - BaseAmount

      val relaxedOutput = quoteAmount + 1 // handle rounding loss
      val fairPrice =
        poolReservesY * BaseAmount * FeeNum <= relaxedOutput * (poolReservesX * FeeDenom + BaseAmount * FeeNum)

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardBox.propositionBytes == Pk.propBytes &&
      quoteAsset._1 == QuoteId &&
      quoteAmount >= MinQuoteAmount &&
      fairDexFee &&
      fairPrice &&
      validMinerFee

    } else false

  sigmaProp(Pk || validTrade)
}
