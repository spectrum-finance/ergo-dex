{
    val InitiallyLockedLP = 0x7fffffffffffffffL

    val poolIn = INPUTS(0)

    val validDeposit =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
            val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

            val poolLP    = poolIn.tokens(1)
            val reservesX = poolIn.tokens(2)
            val reservesY = poolIn.tokens(3)

            val reservesXAmount = reservesX._2
            val reservesYAmount = reservesY._2

            val supplyLP = InitiallyLockedLP - poolLP._2

            val selfX       = SELF.tokens(0)
            val selfY       = SELF.tokens(1)
            val selfXAmount = if (SpectrumIsX) selfX._2 - ExFee else selfX._2
            val selfYAmount = if (SpectrumIsY) selfY._2 - ExFee else selfY._2

            val minByX = selfXAmount.toBigInt * supplyLP / reservesXAmount
            val minByY = selfYAmount.toBigInt * supplyLP / reservesYAmount

            val minimalReward = min(minByX, minByY)

            val rewardOut = OUTPUTS(1)
            val rewardLP  = rewardOut.tokens(0)

            val validErgChange = rewardOut.value >= SELF.value

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