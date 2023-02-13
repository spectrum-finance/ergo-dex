{ // ===== Contract Information ===== //
  // Name: Deposit
  // Description: Contract that validates user's deposit into the CFMM n2t Pool.
  //
  // Constants:
  //
  // {1}  -> SelfXAmount[Long]
  // {2}  -> RefundProp[ProveDlog]
  // {9}  -> SelfYAmount[Long] SELF.tokens(1)._2 - ExFee if Y is SPF else SELF.tokens(1)._2
  // {12} -> PoolNFT[Coll[Byte]]
  // {13} -> RedeemerPropBytes[Coll[Byte]]
  // {18} -> MinerPropBytes[Coll[Byte]]
  // {21} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19bf0417040005c0b80208cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040404060402040205feffffffffffffffff01040405e0d403040004000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010404040205c0b80201000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d609b27203730800d60a7e8c72090206d60b9d9c7e7309067206720ad60cdb63087204d60db2720c730a00ededededed938cb27203730b0001730c93c27204730d95ed8f7208720b93b1720c730ed801d60eb2720c730f00eded92c1720499c1a77310938c720e018c720901927e8c720e02069d9c99720b7208720a720695927208720b927ec1720406997ec1a706997e7202069d9c997208720b720772067311938c720d018c720501927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7312c1720e73137314d9010e599a8c720e018c720e0273157316
  //
  // ErgoTreeTemplate: d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d609b27203730800d60a7e8c72090206d60b9d9c7e7309067206720ad60cdb63087204d60db2720c730a00ededededed938cb27203730b0001730c93c27204730d95ed8f7208720b93b1720c730ed801d60eb2720c730f00eded92c1720499c1a77310938c720e018c720901927e8c720e02069d9c99720b7208720a720695927208720b927ec1720406997ec1a706997e7202069d9c997208720b720772067311938c720d018c720501927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7312c1720e73137314d9010e599a8c720e018c720e0273157316

  val InitiallyLockedLP = 0x7fffffffffffffffL

  val poolIn = INPUTS(0)

  // Validations
  // 1.
  val validDeposit =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {

      val selfXAmount = SelfXAmount
      val selfYAmount = SelfYAmount
      // 1.1.
      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

      val poolLP          = poolIn.tokens(1)
      val reservesXAmount = poolIn.value
      val reservesY       = poolIn.tokens(2)
      val reservesYAmount = reservesY._2

      val supplyLP = InitiallyLockedLP - poolLP._2

      val minByX = selfXAmount.toBigInt * supplyLP / reservesXAmount
      val minByY = selfYAmount.toBigInt * supplyLP / reservesYAmount

      val minimalReward = min(minByX, minByY)

      val rewardOut = OUTPUTS(1)
      val rewardLP  = rewardOut.tokens(0)
      // 1.2.
      val validChange =
        if (minByX < minByY && rewardOut.tokens.size == 2) {
          val diff    = minByY - minByX
          val excessY = diff * reservesYAmount / supplyLP
          val changeY = rewardOut.tokens(1)

          rewardOut.value >= SELF.value - selfXAmount &&
          changeY._1 == reservesY._1 &&
          changeY._2 >= excessY
        } else if (minByX >= minByY) {
          val diff    = minByX - minByY
          val excessX = diff * reservesXAmount / supplyLP

          rewardOut.value >= SELF.value - (selfXAmount - excessX)
        } else {
          false
        }
      // 1.3.
      val validMinerFee = OUTPUTS.map { (o: Box) =>
        if (o.propositionBytes == MinerPropBytes) o.value else 0L
      }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn &&
      rewardOut.propositionBytes == RedeemerPropBytes &&
      validChange &&
      rewardLP._1 == poolLP._1 &&
      rewardLP._2 >= minimalReward &&
      validMinerFee

    } else false

  sigmaProp(RefundProp || validDeposit)
}
