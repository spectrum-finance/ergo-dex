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
  //      3: Redeem blocks delta  // the number of blocks after the end of LM program, at which redeems can be performed without any restrictions.
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
  // ErgoTree: 19e9041f04000402040204040404040604060408040804040402040004000402040204000400040a0500040204020500050004020402040605000500040205000500d81bd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e4c6a70605d607b27202730100d608b27203730200d609b27202730300d60ab27203730400d60bb27202730500d60cb27203730600d60db27202730700d60eb27203730800d60f8c720a02d610998c720902720fd6118c720802d612b27204730900d6139a99a37212730ad614b27204730b00d6159d72137214d61695919e72137214730c9a7215730d7215d617b27204730e00d6187e721705d6199d72057218d61a998c720b028c720c02d61b998c720d028c720e02d1ededededed93b27202730f00b27203731000ededed93e4c672010410720493e4c672010505720593e4c6720106057206928cc77201018cc7a70193c27201c2a7ededed938c7207018c720801938c7209018c720a01938c720b018c720c01938c720d018c720e0193b172027311959172107312eded929a997205721172069c7e9995907216721772169a721773137314057219937210f0721a939c7210997218a273157e721605f0721b958f72107316ededec929a997205721172069c7e9995907216721772169a72177317731805721992a39a9a72129c72177214b2720473190093721af0721092721b959172167217731a9c721a997218a2731b7e721605d801d61ce4c672010704edededed90721c997216731c909972119c7e997217721c0572199a7219720693f0998c72070272117d9d9c7e7219067e721b067e720f0605937210731d93721a731e
  //
  // ErgoTreeTemplate: d81bd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e4c6a70605d607b27202730100d608b27203730200d609b27202730300d60ab27203730400d60bb27202730500d60cb27203730600d60db27202730700d60eb27203730800d60f8c720a02d610998c720902720fd6118c720802d612b27204730900d6139a99a37212730ad614b27204730b00d6159d72137214d61695919e72137214730c9a7215730d7215d617b27204730e00d6187e721705d6199d72057218d61a998c720b028c720c02d61b998c720d028c720e02d1ededededed93b27202730f00b27203731000ededed93e4c672010410720493e4c672010505720593e4c6720106057206928cc77201018cc7a70193c27201c2a7ededed938c7207018c720801938c7209018c720a01938c720b018c720c01938c720d018c720e0193b172027311959172107312eded929a997205721172069c7e9995907216721772169a721773137314057219937210f0721a939c7210997218a273157e721605f0721b958f72107316ededec929a997205721172069c7e9995907216721772169a72177317731805721992a39a9a72129c72177214b2720473190093721af0721092721b959172167217731a9c721a997218a2731b7e721605d801d61ce4c672010704edededed90721c997216731c909972119c7e997217721c0572199a7219720693f0998c72070272117d9d9c7e7219067e721b067e720f0605937210731d93721a731e
  //
  // Validations:
  // 1. LM Pool NFT is preserved;
  // 2. LM Pool Config, LM program budget, maxRoundingError and creationHeight are preserved;
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
  val poolX0   = SELF.tokens(1)
  val poolLQ0  = SELF.tokens(2)
  val poolVLQ0 = SELF.tokens(3)
  val poolTMP0 = SELF.tokens(4)

  val conf0            = SELF.R4[Coll[Int]].get
  val epochLen         = conf0(0)
  val epochNum         = conf0(1)
  val programStart     = conf0(2)
  val redeemLimitDelta = conf0(3)

  val creationHeight0 = SELF.creationInfo._1

  val programBudget0    = SELF.R5[Long].get
  val maxRoundingError0 = SELF.R6[Long].get

  // ===== Getting OUTPUTS data ===== //
  val successor = OUTPUTS(0)

  val poolNFT1 = successor.tokens(0)
  val poolX1   = successor.tokens(1)
  val poolLQ1  = successor.tokens(2)
  val poolVLQ1 = successor.tokens(3)
  val poolTMP1 = successor.tokens(4)

  val creationHeight1 = successor.creationInfo._1
  val conf1           = successor.R4[Coll[Int]].get

  val programBudget1    = successor.R5[Long].get
  val maxRoundingError1 = successor.R6[Long].get

  // ===== Getting deltas ===== //
  val reservesX  = poolX0._2
  val reservesLQ = poolLQ0._2

  val deltaX   = poolX1._2 - reservesX
  val deltaLQ  = poolLQ1._2 - reservesLQ
  val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
  val deltaTMP = poolTMP1._2 - poolTMP0._2

  // ===== Calculating epoch parameters ===== //
  val epochAlloc    = programBudget0 / epochNum
  val curBlockIx    = HEIGHT - programStart + 1
  val curEpochIxRem = curBlockIx % epochLen
  val curEpochIxR   = curBlockIx / epochLen
  val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

  // ===== Validating conditions ===== //
  // 1.
  val nftPreserved = poolNFT1 == poolNFT0
  // 2.
  val configPreserved =
    (conf1 == conf0) &&
    (programBudget1 == programBudget0) &&
    (maxRoundingError1 == maxRoundingError0) &&
    (creationHeight1 >= creationHeight0)

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
      val releasedVLQ     = deltaLQ
      val epochsAllocated = epochNum - max(0L, curEpochIx)
      val releasedTMP     = releasedVLQ * epochsAllocated
      // 6.1.1.
      val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
      val prevEpochsCompoundedForDeposit =
        ((programBudget0 - reservesX) + maxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc

      prevEpochsCompoundedForDeposit &&
      // 6.1.2. && 6.1.3.
      (deltaLQ == -deltaVLQ) &&
      (releasedTMP == -deltaTMP)

    } else if (deltaLQ < 0) { // redeem
      // 6.2.
      val releasedLQ = deltaVLQ
      val minReturnedTMP =
        if (curEpochIx > epochNum) 0L
        else {
          val epochsDeallocated = epochNum - max(0L, curEpochIx)
          releasedLQ * epochsDeallocated
        }
      // 6.2.1.
      val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
      val prevEpochsCompoundedForRedeem =
        ((programBudget0 - reservesX) + maxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc
      val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

      (prevEpochsCompoundedForRedeem || redeemNoLimit) &&
      // 6.2.2. & 6.2.3.
      (deltaVLQ == -deltaLQ) &&
      (deltaTMP >= minReturnedTMP)

    } else { // compound
      // 6.3.
      val epoch               = successor.R7[Int].get
      val epochsToCompound    = epochNum - epoch
      val prevEpochCompounded = (reservesX - epochsToCompound * epochAlloc) <= (epochAlloc + maxRoundingError0)

      val legalEpoch = epoch <= curEpochIx - 1
      val reward     = (epochAlloc.toBigInt * deltaTMP / reservesLQ).toLong

      // 6.3.1. && 6.3.2. && 6.3.3. && 6.3.4. && 6.3.5.
      legalEpoch &&
      prevEpochCompounded &&
      (-deltaX == reward) &&
      (deltaLQ == 0L) &&
      (deltaVLQ == 0L)
    }
  }
  sigmaProp(
    nftPreserved &&
    configPreserved &&
    scriptPreserved &&
    assetsPreserved &&
    noMoreTokens &&
    validAction
  )
}
