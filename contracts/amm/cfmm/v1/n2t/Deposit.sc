{
  val InitiallyLockedLP = 0x7fffffffffffffffL

  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
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
          val diff = minByY - minByX
          val excessY = diff * reservesYAmount / supplyLP

          val changeY = rewardOut.tokens(1)

          rewardOut.value >= SELF.value - DexFee - _selfX &&
            changeY._1 == reservesY._1 &&
            changeY._2 >= excessY
        } else if (minByX >= minByY) {
          val diff = minByX - minByY
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