// Constants:
// ================================
// {1} -> RefundProp[ProveDlog]
// {8} -> SelfXAmount[Long] // SELF.tokens(0)._2 - ExFee
// {10} -> SelfYAmount[Long] // SELF.tokens(1)._2 - ExFee
// {13} -> PoolNFT[Coll[Byte]]
// {14} -> RedeemerPropBytes[Coll[Byte]]
// {21} -> MinerPropBytes[Coll[Byte]]
// {24} -> MaxMinerFee[Long]
// ================================
// ErgoTree: 19a6051a040008cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a040404080402040205feffffffffffffffff01040405d00f040605a01f040004000e2000000000000000000000000000000000000000000000000000000000000000000e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0404040204040402010101000e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0500050005fe887a0100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c7e73080672057207d609b27202730900d60a7e8c72090206d60b9d9c7e730a067205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319
// ================================
// ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c7e73080672057207d609b27202730900d60a7e8c72090206d60b9d9c7e730a067205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319
// ================================
{
    val InitiallyLockedLP = 0x7fffffffffffffffL

    val SelfXAmount = 1000L
    val SelfYAmount = 2000L

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

            val minByX = SelfXAmount.toBigInt * supplyLP / reservesXAmount
            val minByY = SelfYAmount.toBigInt * supplyLP / reservesYAmount

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
            rewardOut.propositionBytes == RedeemerPropBytes &&
            validErgChange &&
            validTokenChange &&
            rewardLP._1 == poolLP._1 &&
            rewardLP._2 >= minimalReward &&
            validMinerFee
        } else false

    sigmaProp(RefundProp || validDeposit)
}
