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

            val supplyLP = InitiallyLockedLP - poolLP._2

            val minimalReward = min(
                SelfX.toBigInt * supplyLP / reservesXAmount,
                selfY._2.toBigInt * supplyLP / reservesY._2
            )

            val rewardOut = OUTPUTS(1)
            val rewardLP  = rewardOut.tokens(0)

            validPoolIn &&
            rewardOut.propositionBytes == Pk.propBytes &&
            rewardOut.value >= SELF.value - DexFee - SelfX &&
            rewardLP._1 == poolLP._1 &&
            rewardLP._2 >= minimalReward
        } else false

    sigmaProp(Pk || validDeposit)
}