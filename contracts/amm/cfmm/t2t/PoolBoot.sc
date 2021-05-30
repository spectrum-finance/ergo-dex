{
    val poolScriptHash  = SELF.R5[Coll[Byte]].get
    val desiredSharesLP = SELF.R6[Long].get
    val poolFeeConfig   = SELF.R7[Long].get
    val minerFeeNErgs   = SELF.R8[Long].get
    val initiatorProp   = SELF.R9[Coll[Byte]].get

    val selfLP = SELF.tokens(0)
    val selfX  = SELF.tokens(1)
    val selfY  = SELF.tokens(2)

    val tokenIdLP = selfLP._1

    val pool           = OUTPUTS(0)
    val sharesRewardLP = OUTPUTS(1)

    val maybePoolLP  = pool.tokens(1)
    val poolAmountLP =
        if (maybePoolLP._1 == tokenIdLP) maybePoolLP._2
        else 0L

    val validPoolContract  = blake2b256(pool.propositionBytes) == poolScriptHash
    val validPoolErgAmount = pool.value == SELF.value - sharesRewardLP.value - minerFeeNErgs
    val validPoolNFT       = pool.tokens(0) == (SELF.id, 1L)
    val validPoolConfig    = pool.R4[Long].get == poolFeeConfig 

    val validInitialDepositing = {
        val tokenX     = pool.tokens(2)
        val tokenY     = pool.tokens(3)
        val depositedX = tokenX._2
        val depositedY = tokenY._2

        val validTokens  = tokenX == selfX && tokenY == selfY
        val validDeposit = depositedX.toBigInt * depositedY >= desiredSharesLP.toBigInt * desiredSharesLP // S >= sqrt(X_deposited * Y_deposited) Deposits satisfy desired share
        val validShares  = poolAmountLP >= (InitiallyLockedLP - desiredSharesLP)                          // valid amount of liquidity shares taken from reserves
        
        validTokens && validDeposit && validShares
    }

    val validPool = validPoolContract && validPoolErgAmount && validPoolNFT && validInitialDepositing

    val validSharesRewardLP =
        sharesRewardLP.propositionBytes == initiatorProp &&
        sharesRewardLP.tokens(0) == (tokenIdLP, desiredSharesLP)
    
    sigmaProp(validSelfLP && validSelfPoolFeeConfig && validPool && validSharesRewardLP)
}