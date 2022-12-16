{
    val InitiallyLockedLP = 0x7fffffffffffffffL

    val selfLP = SELF.tokens(0)

    val poolIn = INPUTS(0)

    val validRedeem =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
            val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

            val poolLP    = poolIn.tokens(1)
            val reservesX = poolIn.tokens(2)
            val reservesY = poolIn.tokens(3)

            val supplyLP = InitiallyLockedLP - poolLP._2

            val minReturnX = selfLP._2.toBigInt * reservesX._2 / supplyLP
            val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP

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