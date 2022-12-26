// Constants:
// ================================
// {1} -> SelfXAmount[Long]
// {2} -> RefundProp[ProveDlog]
// {11} -> ExFee[Long]
// {14} -> PoolNFT[Coll[Byte]]
// {15} -> RedeemerPropBytes[Coll[Byte]]
// {20} -> MinerPropBytes[Coll[Byte]]
// {23} -> MaxMinerFee[Long]
// ================================
// ErgoTree: 199f0519040004f00d08cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a040404060402040205feffffffffffffffff01040004040101059a05040004000e2000000000000000000000000000000000000000000000000000000000000000000e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0404040204f00d01000e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0500050005fe887a0100d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80cd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d6098cb2db6308a773080002d60ab27203730900d60b7e8c720a0206d60c9d9c7e95730a997209730b7209067206720bd60ddb63087204d60eb2720d730c00ededededed938cb27203730d0001730e93c27204730f95ed8f7208720c93b1720d7310d801d60fb2720d731100eded92c1720499c1a77e731205938c720f018c720a01927e8c720f02069d9c99720c7208720b720695927208720c927ec1720406997ec1a706997e7202069d9c997208720c720772067313938c720e018c720501927e8c720e0206a17208720c90b0ada5d9010f639593c2720f7314c1720f73157316d9010f599a8c720f018c720f0273177318
// ================================
// ErgoTreeTemplate: d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80cd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d6098cb2db6308a773080002d60ab27203730900d60b7e8c720a0206d60c9d9c7e95730a997209730b7209067206720bd60ddb63087204d60eb2720d730c00ededededed938cb27203730d0001730e93c27204730f95ed8f7208720c93b1720d7310d801d60fb2720d731100eded92c1720499c1a77e731205938c720f018c720a01927e8c720f02069d9c99720c7208720b720695927208720c927ec1720406997ec1a706997e7202069d9c997208720c720772067313938c720e018c720501927e8c720e0206a17208720c90b0ada5d9010f639593c2720f7314c1720f73157316d9010f599a8c720f018c720f0273177318
// ================================
{
  val InitiallyLockedLP = 0x7fffffffffffffffL

  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
      val selfY = SELF.tokens(0)

      val selfXAmount = SelfXAmount
      val selfYAmount = if (SpectrumIsY) selfY._2 - ExFee else selfY._2

      val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

      val poolLP = poolIn.tokens(1)
      val reservesXAmount = poolIn.value
      val reservesY = poolIn.tokens(2)
      val reservesYAmount = reservesY._2

      val supplyLP = InitiallyLockedLP - poolLP._2

      val minByX = selfXAmount.toBigInt * supplyLP / reservesXAmount
      val minByY = selfYAmount.toBigInt * supplyLP / reservesYAmount

      val minimalReward = min(minByX, minByY)

      val rewardOut = OUTPUTS(1)
      val rewardLP = rewardOut.tokens(0)

      val validChange =
        if (minByX < minByY && rewardOut.tokens.size == 2) {
          val diff = minByY - minByX
          val excessY = diff * reservesYAmount / supplyLP
          val changeY = rewardOut.tokens(1)

          rewardOut.value >= SELF.value - selfXAmount &&
            changeY._1 == reservesY._1 &&
            changeY._2 >= excessY
        } else if (minByX >= minByY) {
          val diff = minByX - minByY
          val excessX = diff * reservesXAmount / supplyLP

          rewardOut.value >= SELF.value - (selfXAmount - excessX)
        } else {
          false
        }

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