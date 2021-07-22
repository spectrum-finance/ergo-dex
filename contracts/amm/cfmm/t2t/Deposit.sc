{
    val selfX = SELF.tokens(0)
    val selfY = SELF.tokens(1)

    val poolIn = INPUTS(0)

    val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)

    val poolLP    = poolIn.tokens(1)
    val reservesX = poolIn.tokens(2)
    val reservesY = poolIn.tokens(2)

    val supplyLP = InitiallyLockedLP - poolLP._2

    val minimalReward = min(
        selfX._2.toBigInt * supplyLP / reservesX._2,
        selfY._2.toBigInt * supplyLP / reservesY._2
    )

    val rewardOut = OUTPUTS(1)
    val rewardLP  = rewardOut.tokens(0)

    val uniqueOutput = rewardOut.R4[Int].map({(i: Int) => INPUTS(i).id == SELF.id}).getOrElse(false)

    val validRewardOut =
        rewardOut.propositionBytes == Pk.propBytes &&
        rewardOut.value >= SELF.value - DexFee &&
        rewardLP._1 == poolLP._1 &&
        rewardLP._2 >= minimalReward &&
        uniqueOutput

    sigmaProp(Pk || (validPoolIn && validRewardOut))
}