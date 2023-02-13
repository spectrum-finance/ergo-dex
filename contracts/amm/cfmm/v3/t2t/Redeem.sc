{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the CFMM t2t Pool.
  //
  // Constants:
  //
  // {1}  -> RefundProp[ProveDlog]
  // {13} -> PoolNFT[Coll[Byte]]
  // {14} -> RedeemerPropBytes[Coll[Byte]]
  // {15} -> MinerPropBytes[Coll[Byte]]
  // {18} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19e60314040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040804020400040404020406040005feffffffffffffffff01040204000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313
  //
  // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313

  val InitiallyLockedLP = 0x7fffffffffffffffL

  val selfLP = SELF.tokens(0)

  val poolIn = INPUTS(0)

  // Validations
  // 1.
  val validRedeem =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

      val poolLP    = poolIn.tokens(1)
      val reservesX = poolIn.tokens(2)
      val reservesY = poolIn.tokens(3)

      val supplyLP = InitiallyLockedLP - poolLP._2

      val selfLPAmount = selfLP._2.toBigInt
      val minReturnX   = selfLPAmount * reservesX._2 / supplyLP
      val minReturnY   = selfLPAmount * reservesY._2 / supplyLP

      val returnOut = OUTPUTS(1)

      val returnX = returnOut.tokens(0)
      val returnY = returnOut.tokens(1)

      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      returnOut.propositionBytes == RedeemerPropBytes &&
      returnX._1 == reservesX._1 &&
      returnY._1 == reservesY._1 &&
      returnX._2 >= minReturnX &&
      returnY._2 >= minReturnY &&
      validMinerFee

    } else false

  sigmaProp(RefundProp || validRedeem)
}
