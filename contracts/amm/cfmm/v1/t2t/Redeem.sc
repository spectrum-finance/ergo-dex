{
  // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem into the CFMM t2t Pool.
  //
  // ErgoTree: 19b9031508d204000404040804020400040404020406040005feffffffffffffffff01040204000e200202020202020202020202020202020202020202020202020202020202020202050205ca0f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d809d603db63087202d604b2a5730400d605db63087204d606b27205730500d607b27203730600d608b27205730700d609b27203730800d60a7e8cb2db6308a77309000206d60b7e99730a8cb27203730b000206ededededededed93b27203730c008602730d730e93c27204d0720192c1720499c1a7730f938c7206018c720701938c7208018c720901927e8c720602069d9c720a7e8c72070206720b927e8c720802069d9c720a7e8c72090206720b90b0ada5d9010c639593c2720c7310c1720c73117312d9010c599a8c720c018c720c0273137314
  //
  // ErgoTreeTemplate: d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d809d603db63087202d604b2a5730400d605db63087204d606b27205730500d607b27203730600d608b27205730700d609b27203730800d60a7e8cb2db6308a77309000206d60b7e99730a8cb27203730b000206ededededededed93b27203730c008602730d730e93c27204d0720192c1720499c1a7730f938c7206018c720701938c7208018c720901927e8c720602069d9c720a7e8c72070206720b927e8c720802069d9c720a7e8c72090206720b90b0ada5d9010c639593c2720c7310c1720c73117312d9010c599a8c720c018c720c0273137314

  val InitiallyLockedLP = 0x7fffffffffffffffL

  val selfLP = SELF.tokens(0)

  val poolIn = INPUTS(0)

  val validRedeem =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
      val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)

      val poolLP    = poolIn.tokens(1)
      val reservesX = poolIn.tokens(2)
      val reservesY = poolIn.tokens(3)

      val supplyLP = InitiallyLockedLP - poolLP._2

      val minReturnX = selfLP._2.toBigInt * reservesX._2 / supplyLP
      val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP

      val returnOut = OUTPUTS(1)

      val returnX = returnOut.tokens(0)
      val returnY = returnOut.tokens(1)

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      returnOut.propositionBytes == Pk.propBytes &&
      returnOut.value >= SELF.value - DexFee &&
      returnX._1 == reservesX._1 &&
      returnY._1 == reservesY._1 &&
      returnX._2 >= minReturnX &&
      returnY._2 >= minReturnY &&
      validMinerFee

    } else false

  sigmaProp(Pk || validRedeem)
}
