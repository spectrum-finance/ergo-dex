{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the CFMM n2t Pool.
  //
  // ErgoTree: 198d031208d2040004040406040204000404040005feffffffffffffffff01040204000e20020202020202020202020202020202020202020202020202020202020202020205ca0f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d806d603db63087202d604b2a5730400d605b2db63087204730500d606b27203730600d6077e8cb2db6308a77307000206d6087e9973088cb272037309000206ededededed938cb27203730a0001730b93c27204d07201938c7205018c720601927e9a99c17204c1a7730c069d9c72077ec17202067208927e8c720502069d9c72077e8c72060206720890b0ada5d90109639593c27209730dc17209730e730fd90109599a8c7209018c72090273107311
  //
  // ErgoTreeTemplate: d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d806d603db63087202d604b2a5730400d605b2db63087204730500d606b27203730600d6077e8cb2db6308a77307000206d6087e9973088cb272037309000206ededededed938cb27203730a0001730b93c27204d07201938c7205018c720601927e9a99c17204c1a7730c069d9c72077ec17202067208927e8c720502069d9c72077e8c72060206720890b0ada5d90109639593c27209730dc17209730e730fd90109599a8c7209018c72090273107311

  val InitiallyLockedLP = 0x7fffffffffffffffL

  val poolIn = INPUTS(0)

  val validRedeem =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
      val selfLP = SELF.tokens(0)

      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

      val poolLP          = poolIn.tokens(1)
      val reservesXAmount = poolIn.value
      val reservesY       = poolIn.tokens(2)

      val supplyLP = InitiallyLockedLP - poolLP._2

      val minReturnX = selfLP._2.toBigInt * reservesXAmount / supplyLP
      val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP

      val returnOut = OUTPUTS(1)

      val returnXAmount = returnOut.value - SELF.value + DexFee
      val returnY       = returnOut.tokens(0)

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
        returnOut.propositionBytes == Pk.propBytes &&
        returnY._1 == reservesY._1 && // token id matches
        returnXAmount >= minReturnX &&
        returnY._2 >= minReturnY &&
        validMinerFee

    } else false

  sigmaProp(Pk || validRedeem)
}