{ // ===== Contract Information ===== //
  // Name: Deposit
  // Description: Contract that validates user's deposit into the CFMM n2t Pool.
  //
  // ErgoTree: 1993041808d2040005be9a0c040404060402040205feffffffffffffffff0104040400040004000e2002020202020202020202020202020202020202020202020202020202020202020404040205ca0f05be9a0c05ca0f01000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d803d6017300d602b2a4730100d6037302eb027201d195ed92b1a4730393b1db630872027304d80bd604db63087202d605b2a5730500d606b27204730600d6077e9973078c72060206d6087ec1720206d6099d9c7e72030672077208d60ab27204730800d60b7e8c720a0206d60c9d9c7e8cb2db6308a773090002067207720bd60ddb63087205d60eb2720d730a00ededededed938cb27204730b0001730c93c27205d0720195ed8f7209720c93b1720d730dd801d60fb2720d730e00eded92c172059999c1a7730f7310938c720f018c720a01927e8c720f02069d9c99720c7209720b720795927209720c927ec1720506997e99c1a7731106997e7203069d9c997209720c720872077312938c720e018c720601927e8c720e0206a17209720c90b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317
  //
  // ErgoTreeTemplate: d803d6017300d602b2a4730100d6037302eb027201d195ed92b1a4730393b1db630872027304d80bd604db63087202d605b2a5730500d606b27204730600d6077e9973078c72060206d6087ec1720206d6099d9c7e72030672077208d60ab27204730800d60b7e8c720a0206d60c9d9c7e8cb2db6308a773090002067207720bd60ddb63087205d60eb2720d730a00ededededed938cb27204730b0001730c93c27205d0720195ed8f7209720c93b1720d730dd801d60fb2720d730e00eded92c172059999c1a7730f7310938c720f018c720a01927e8c720f02069d9c99720c7209720b720795927209720c927ec1720506997e99c1a7731106997e7203069d9c997209720c720872077312938c720e018c720601927e8c720e0206a17209720c90b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317

  val InitiallyLockedLP = 0x7fffffffffffffffL

  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
      val selfY = SELF.tokens(0)

      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

      val poolLP          = poolIn.tokens(1)
      val reservesXAmount = poolIn.value
      val reservesY       = poolIn.tokens(2)
      val reservesYAmount = reservesY._2

      val supplyLP = InitiallyLockedLP - poolLP._2

      val _selfX = SelfX

      val minByX = _selfX.toBigInt * supplyLP / reservesXAmount
      val minByY = selfY._2.toBigInt * supplyLP / reservesYAmount

      val minimalReward = min(minByX, minByY)

      val rewardOut = OUTPUTS(1)
      val rewardLP  = rewardOut.tokens(0)

      val validChange =
        if (minByX < minByY && rewardOut.tokens.size == 2) {
          val diff    = minByY - minByX
          val excessY = diff * reservesYAmount / supplyLP

          val changeY = rewardOut.tokens(1)

          rewardOut.value >= SELF.value - DexFee - _selfX &&
          changeY._1 == reservesY._1 &&
          changeY._2 >= excessY

        } else if (minByX >= minByY) {
          val diff    = minByX - minByY
          val excessX = diff * reservesXAmount / supplyLP

          rewardOut.value >= SELF.value - DexFee - (_selfX - excessX)

        } else {
          false
        }

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardOut.propositionBytes == Pk.propBytes &&
      validChange &&
      rewardLP._1 == poolLP._1 &&
      rewardLP._2 >= minimalReward &&
      validMinerFee

    } else false

  sigmaProp(Pk || validDeposit)
}
