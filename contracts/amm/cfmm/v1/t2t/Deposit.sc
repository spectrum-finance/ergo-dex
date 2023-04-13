{
  // ===== Contract Information ===== //
  // Name: Deposit
  // Description: Contract that validates user's deposit into the CFMM t2t Pool.
  //
  // ErgoTree: 19b9041b08d20400040404080402040205feffffffffffffffff010404040004060402040004000e200202020202020202020202020202020202020202020202020202020202020202050205ca0f0404040204040402010101000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d803d6017300d602b2a4730100d603db6308a7eb027201d195ed92b1a4730293b1db630872027303d80cd604db63087202d605b2a5730400d606b27204730500d6077e9973068c72060206d608b27204730700d6097e8c72080206d60a9d9c7e8cb27203730800020672077209d60bb27204730900d60c7e8c720b0206d60d9d9c7e8cb27203730a0002067207720cd60edb63087205d60fb2720e730b00edededededed93b27204730c008602730d730e93c27205d0720192c1720599c1a7730f95ed8f720a720d93b1720e7310d801d610b2720e731100ed938c7210018c720b01927e8c721002069d9c99720d720a720c720795ed91720a720d93b1720e7312d801d610b2720e731300ed938c7210018c720801927e8c721002069d9c99720a720d720972079593720a720d73147315938c720f018c720601927e8c720f0206a1720a720d90b0ada5d90110639593c272107316c1721073177318d90110599a8c7210018c7210027319731a
  //
  // ErgoTreeTemplate: d803d6017300d602b2a4730100d603db6308a7eb027201d195ed92b1a4730293b1db630872027303d80cd604db63087202d605b2a5730400d606b27204730500d6077e9973068c72060206d608b27204730700d6097e8c72080206d60a9d9c7e8cb27203730800020672077209d60bb27204730900d60c7e8c720b0206d60d9d9c7e8cb27203730a0002067207720cd60edb63087205d60fb2720e730b00edededededed93b27204730c008602730d730e93c27205d0720192c1720599c1a7730f95ed8f720a720d93b1720e7310d801d610b2720e731100ed938c7210018c720b01927e8c721002069d9c99720d720a720c720795ed91720a720d93b1720e7312d801d610b2720e731300ed938c7210018c720801927e8c721002069d9c99720a720d720972079593720a720d73147315938c720f018c720601927e8c720f0206a1720a720d90b0ada5d90110639593c272107316c1721073177318d90110599a8c7210018c7210027319731a

  val InitiallyLockedLP = 0x7fffffffffffffffL

  val selfX = SELF.tokens(0)
  val selfY = SELF.tokens(1)

  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
      val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)

      val poolLP    = poolIn.tokens(1)
      val reservesX = poolIn.tokens(2)
      val reservesY = poolIn.tokens(3)

      val reservesXAmount = reservesX._2
      val reservesYAmount = reservesY._2

      val supplyLP = InitiallyLockedLP - poolLP._2

      val minByX = selfX._2.toBigInt * supplyLP / reservesXAmount
      val minByY = selfY._2.toBigInt * supplyLP / reservesYAmount

      val minimalReward = min(minByX, minByY)

      val rewardOut = OUTPUTS(1)
      val rewardLP  = rewardOut.tokens(0)

      val validErgChange = rewardOut.value >= SELF.value - DexFee

      val validTokenChange =
        if (minByX < minByY && rewardOut.tokens.size == 2) {
          val diff    = minByY - minByX
          val excessY = diff * reservesYAmount / supplyLP

          val changeY = rewardOut.tokens(1)

          changeY._1 == reservesY._1 &&
          changeY._2 >= excessY

        } else if (minByX > minByY && rewardOut.tokens.size == 2) {
          val diff    = minByX - minByY
          val excessX = diff * reservesXAmount / supplyLP

          val changeX = rewardOut.tokens(1)

          changeX._1 == reservesX._1 &&
          changeX._2 >= excessX

        } else if (minByX == minByY) {
          true

        } else {
          false
        }

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardOut.propositionBytes == Pk.propBytes &&
      validErgChange &&
      validTokenChange &&
      rewardLP._1 == poolLP._1 &&
      rewardLP._2 >= minimalReward &&
      validMinerFee

    } else false

  sigmaProp(Pk || validDeposit)
}
