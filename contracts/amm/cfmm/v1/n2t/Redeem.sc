{
    val InitiallyLockedLP = 0x7fffffffffffffffL

    val poolIn = INPUTS(0)

    val validRedeem =
        if (INPUTS.size == 2 && poolIn.tokens.size == 3) {
            val selfLP = SELF.tokens(0)

            val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

            val poolLP          = poolIn.tokens(1)
            val reservesXAmount = poolIn.value
            val reservesY       = poolIn.tokens(2)

            val supplyLP = InitiallyLockedLP - poolLP._2

            val minReturnX = selfLP._2.toBigInt * reservesXAmount / supplyLP
            val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP

            val returnOut = OUTPUTS(1)

            val returnXAmount = returnOut.value - SELF.value + DexFee
            val returnY       = returnOut.tokens(0)

            validPoolIn &&
            returnOut.propositionBytes == Pk.propBytes &&
            returnOut.value >= SELF.value - DexFee &&
            returnY._1 == reservesY._1 && // token id matches
            returnXAmount >= minReturnX &&
            returnY._2 >= minReturnY
        } else false

    sigmaProp(Pk || validRedeem)
}