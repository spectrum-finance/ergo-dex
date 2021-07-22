{
    val selfLP = SELF.tokens(0)

    val poolIn = INPUTS(0)

    val validPoolIn = poolIn.tokens(0) == (PoolNFT, 1L)

    val poolLP    = poolIn.tokens(1)
    val reservesX = poolIn.tokens(2)
    val reservesY = poolIn.tokens(2)

    val supplyLP = InitiallyLockedLP - poolLP._2

    val minReturnX = selfLP._2.toBigInt * reservesX._2 / supplyLP
    val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP

    val returnOut = OUTPUTS(1)

    val returnX = returnOut.tokens(0)
    val returnY = returnOut.tokens(1)

    val uniqueOutput = returnOut.R4[Int].map({(i: Int) => INPUTS(i).id == SELF.id}).getOrElse(false)

    val validReturnOut =
        returnOut.propositionBytes == Pk.propBytes &&
        returnOut.value >= SELF.value - DexFee &&
        returnX._1 == reservesX._1 &&
        returnY._1 == reservesY._1 &&
        returnX._2 >= minReturnX &&
        returnY._2 >= minReturnY &&
        uniqueOutput

    sigmaProp(Pk || (validPoolIn && validReturnOut))
}