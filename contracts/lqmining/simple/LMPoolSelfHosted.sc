{ // ===== Contract Information ===== //
  // Name: LMPoolSelfHosted
  // Description: Contract that validates a change in the LM pool's state.
  //
  // ===== LM Pool Box ===== //
  // Registers:
  //   R4[Coll[Int]]: LM program config
  //      0: Length of every epoch in blocks
  //      1: Number of epochs in the LM program
  //      2: Program start
  //   R5[Long]: Program budget  // total budget of LM program.
  //   R6[Long]: MaxRoundingError // Tokens rounding delta max value.
  //   R7[Int]: Epoch index  // index of the epoch being compounded (required only for compounding).
  //
  // Tokens:
  //   0:
  //     _1: LM Pool NFT
  //     _2: Amount: 1
  //   1:
  //     _1: Reward Token ID
  //     _2: Amount: <= Program budget.
  //   2:
  //     _1: LQ Token ID  // locked LQ tokens.
  //     _2: Amount of LQ tokens.
  //   3:
  //     _1: vLQ Token ID  // tokens representing locked share of LQ.
  //     _2: Amount of vLQ tokens.
  //   4:
  //     _1: TMP Token ID  // left program epochs times liquidity.
  //     _2: Amount of TMP tokens.
  //
  // ErgoTree:         19d2041e04000402040204040404040604060408040804040402040004000402040204000400
  //                   040a050004020402050005000402040205000500040205000500d81ad601b2a5730000d602db
  //                   63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e4c6a70605d607b272027301
  //                   00d608b27203730200d609b27202730300d60ab27203730400d60bb27202730500d60cb27203
  //                   730600d60db27202730700d60eb27203730800d60f8c720a02d610998c720902720fd6118c72
  //                   0802d6129a99a3b27204730900730ad613b27204730b00d6149d72127213d61595919e721272
  //                   13730c9a7214730d7214d616b27204730e00d6177e721605d6189d72057217d619998c720b02
  //                   8c720c02d61a998c720d028c720e02d1ededededed93b27202730f00b27203731000eded93e4
  //                   c672010410720493e4c672010505720593e4c672010605720693c27201c2a7ededed938c7207
  //                   018c720801938c7209018c720a01938c720b018c720c01938c720d018c720e0193b172027311
  //                   959172107312ededec929a997205721172069c7e9995907215721672159a7216731373140572
  //                   189372117205937210f07219939c7210997217a273157e721505f0721a958f72107316ededec
  //                   929a997205721172069c7e9995907215721672159a7216731773180572189372117205937219
  //                   f0721092721a95917215721673199c7219997217a2731a7e721505d801d61be4c672010704ed
  //                   ededed90721b997215731b909972119c7e997216721b0572189a7218720693f0998c72070272
  //                   117d9d9c7e7218067e721a067e720f0605937210731c937219731d
  //
  // ErgoTreeTemplate: d81ad601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606
  //                   e4c6a70605d607b27202730100d608b27203730200d609b27202730300d60ab27203730400d6
  //                   0bb27202730500d60cb27203730600d60db27202730700d60eb27203730800d60f8c720a02d6
  //                   10998c720902720fd6118c720802d6129a99a3b27204730900730ad613b27204730b00d6149d
  //                   72127213d61595919e72127213730c9a7214730d7214d616b27204730e00d6177e721605d618
  //                   9d72057217d619998c720b028c720c02d61a998c720d028c720e02d1ededededed93b2720273
  //                   0f00b27203731000eded93e4c672010410720493e4c672010505720593e4c672010605720693
  //                   c27201c2a7ededed938c7207018c720801938c7209018c720a01938c720b018c720c01938c72
  //                   0d018c720e0193b172027311959172107312ededec929a997205721172069c7e999590721572
  //                   1672159a7216731373140572189372117205937210f07219939c7210997217a273157e721505
  //                   f0721a958f72107316ededec929a997205721172069c7e9995907215721672159a7216731773
  //                   180572189372117205937219f0721092721a95917215721673199c7219997217a2731a7e7215
  //                   05d801d61be4c672010704edededed90721b997215731b909972119c7e997216721b0572189a
  //                   7218720693f0998c72070272117d9d9c7e7218067e721a067e720f0605937210731c93721973
  //                   1d
  //
  // Validations:
  // 1. LM Pool NFT is preserved;
  // 2. LM Pool Config, LM program budget and minValue are preserved;
  // 3. LMPool validation script is preserved;
  // 4. LM Pool assets are preserved;
  // 5. There are no illegal tokens in LM Pool;
  // 6. Action is valid:
  //    6.1. Deposit: if (deltaLQ > 0)
  //         6.1.1. Previous epochs are compounded;
  //         6.1.2. Delta LQ tokens amount is correct;
  //         6.1.3. Delta TMP tokens amount is correct.
  //    6.2. Redeem: elif if (deltaLQ < 0)
  //         6.2.1. Previous epochs are compounded;
  //         6.2.2. Delta LQ tokens amount is correct;
  //         6.2.3. Delta TMP tokens amount is correct.
  //    6.3. Compound: else
  //         6.3.1. Epoch is legal to perform compounding;
  //         6.3.2. Previous epoch is compounded;
  //         6.3.3. Delta reward tokens amount equals to calculated reward amount;
  //         6.3.4. Delta LQ tokens amount is 0;
  //         6.3.5. Delta vLQ tokens amount is 0.
  //
  // ===== Getting SELF data ===== //
  val poolNFT0 = SELF.tokens(0)
  val poolX0 = SELF.tokens(1)
  val poolLQ0 = SELF.tokens(2)
  val poolVLQ0 = SELF.tokens(3)
  val poolTMP0 = SELF.tokens(4)

  val conf0 = SELF.R4[Coll[Int]].get
  val epochLen = conf0(0)
  val epochNum = conf0(1)
  val programStart = conf0(2)

  val programBudget0 = SELF.R5[Long].get
  val MaxRoundingError0 = SELF.R6[Long].get

  // ===== Getting OUTPUTS data ===== //
  val successor = OUTPUTS(0)

  val poolNFT1 = successor.tokens(0)
  val poolX1 = successor.tokens(1)
  val poolLQ1 = successor.tokens(2)
  val poolVLQ1 = successor.tokens(3)
  val poolTMP1 = successor.tokens(4)

  val conf1 = successor.R4[Coll[Int]].get

  val programBudget1 = successor.R5[Long].get
  val MaxRoundingError1 = successor.R6[Long].get

  // ===== Getting deltas ===== //
  val reservesX = poolX0._2
  val reservesLQ = poolLQ0._2

  val deltaX = poolX1._2 - reservesX
  val deltaLQ = poolLQ1._2 - reservesLQ
  val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
  val deltaTMP = poolTMP1._2 - poolTMP0._2

  // ===== Calculating epoch parameters ===== //
  val epochAlloc = programBudget0 / epochNum
  val curBlockIx = HEIGHT - programStart + 1
  val curEpochIxRem = curBlockIx % epochLen
  val curEpochIxR = curBlockIx / epochLen
  val curEpochIx = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

  // ===== Validating conditions ===== //
  // 1.
  val nftPreserved = poolNFT1 == poolNFT0
  // 2.
  val configPreserved =
    (conf1 == conf0) &&
      (programBudget1 == programBudget0) &&
      (MaxRoundingError1 == MaxRoundingError0)

  // 3.
  val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
  // 4.
  val assetsPreserved =
    poolX1._1 == poolX0._1 &&
      poolLQ1._1 == poolLQ0._1 &&
      poolVLQ1._1 == poolVLQ0._1 &&
      poolTMP1._1 == poolTMP0._1
  // 5.
  val noMoreTokens = successor.tokens.size == 5
  // 6.
  val validAction = {
    if (deltaLQ > 0) { // deposit
      // 6.1.
      val releasedVLQ = deltaLQ
      val epochsAllocated = epochNum - max(0L, curEpochIx)
      val releasedTMP = releasedVLQ * epochsAllocated
      // 6.1.1.
      val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
      val prevEpochsCompoundedForDeposit = ((programBudget0 - reservesX) + MaxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc

      (prevEpochsCompoundedForDeposit || (reservesX == programBudget0)) &&
        // 6.1.2. && 6.1.3.
        (deltaLQ == -deltaVLQ) &&
        (releasedTMP == -deltaTMP)

    } else if (deltaLQ < 0) { // redeem
      // 6.2.
      val releasedLQ = deltaVLQ
      val minReturnedTMP = {
        if (curEpochIx > epochNum) 0L
        else {
          val epochsDeallocated = epochNum - max(0L, curEpochIx)
          releasedLQ * epochsDeallocated
        }
      }
      // 6.2.1.
      val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
      val prevEpochsCompoundedForRedeem = ((programBudget0 - reservesX) + MaxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc

      (prevEpochsCompoundedForRedeem || (reservesX == programBudget0)) &&
        // 6.2.2. & 6.2.3.
        (deltaVLQ == -deltaLQ) &&
        (deltaTMP >= minReturnedTMP)

    }
    else { // compound
      // 6.3.
      val epoch = successor.R7[Int].get
      val epochsToCompound = epochNum - epoch
      val prevEpochCompounded = (reservesX - epochsToCompound * epochAlloc) <= (epochAlloc + MaxRoundingError0)

      val legalEpoch = epoch <= curEpochIx - 1
      val reward = (epochAlloc.toBigInt * deltaTMP / reservesLQ).toLong

      // 6.3.1. && 6.3.2. && 6.3.3. && 6.3.4. && 6.3.5.
      legalEpoch &&
        prevEpochCompounded &&
        (-deltaX == reward) &&
        (deltaLQ == 0L) &&
        (deltaVLQ == 0L)
    }
  }
  sigmaProp(nftPreserved &&
    configPreserved &&
    scriptPreserved &&
    assetsPreserved &&
    noMoreTokens &&
    validAction)
}