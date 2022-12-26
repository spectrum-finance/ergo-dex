// Constants:
// ================================
// {1} -> RefundProp[ProveDlog]
// {13} -> PoolNFT[Coll[Byte]]
// {14} -> RedeemerPropBytes[Coll[Byte]]
// {15} -> MinerPropBytes[Coll[Byte]]
// {18} -> MaxMinerFee[Long]
// ================================
// ErgoTree: 19af0414040008cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a0404040804020400040404020406040005feffffffffffffffff01040204000e2000000000000000000000000000000000000000000000000000000000000000000e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0500050005fe887a0100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313
// ================================
// ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313

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

            val selfLPAmount = selfLP._2.toBigInt
            val minReturnX   = selfLPAmount * reservesX._2 / supplyLP
            val minReturnY   = selfLPAmount * reservesY._2 / supplyLP

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