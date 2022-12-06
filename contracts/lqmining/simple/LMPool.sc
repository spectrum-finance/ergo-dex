{ // ===== Contract Information ===== //
  // Name: LMPool
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
  //   R6[Long]: Max Rounding Error // Tokens rounding delta max value.
  //   R7[Long]: Execution budget  // total execution budget.
  //   R8[Int]: Epoch index  // index of the epoch being compounded (required only for compounding).
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
  // ErgoTree:         19c405230400040204020404040404060406040804080404040204000400040204020400040
  //                   0040a05000402040205000500040204020406050005000402050005000500050005000500d8
  //                   1dd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e
  //                   4c6a70705d607e4c6a70605d608b27202730100d609b27203730200d60ab27202730300d60b
  //                   b27203730400d60cb27202730500d60db27203730600d60eb27202730700d60fb2720373080
  //                   0d6108c720b02d611998c720a027210d6128c720902d613b27204730900d6149a99a3721373
  //                   0ad615b27204730b00d6169d72147215d61795919e72147215730c9a7216730d7216d618b27
  //                   204730e00d6197e721805d61a9d72057219d61b998c720c028c720d02d61c998c720e028c72
  //                   0f02d61d998c7208027212d1ededededed93b27202730f00b27203731000ededed93e4c6720
  //                   10410720493e4c672010505720593e4c672010705720693e4c672010605720793c27201c2a7
  //                   ededed938c7208018c720901938c720a018c720b01938c720c018c720d01938c720e018c720
  //                   f0193b172027311959172117312eded929a997205721272079c7e9995907217721872179a72
  //                   187313731405721a937211f0721b939c7211997219a273157e721705f0721c958f72117316e
  //                   dedec929a997205721272079c7e9995907217721872179a72187317731805721a92a39a9a72
  //                   139c72187215b2720473190093721bf0721192721c959172177218731a9c721b997219a2731
  //                   b7e721705d802d61ec17201d61fc1a79590721e721fd802d620e4c672010804d6217d9d9c7e
  //                   721a067e721c067e72100605ededededed907220997217731c909972129c7e9972187220057
  //                   21a9a721a720793f0721d7221937211731d93721b731e9099721f721e7d9d9c7e7221067e72
  //                   06067e72050605edededed91721e721f937211731f93721b732093721d732193721c7322
  //
  // ErgoTreeTemplate: d81dd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606
  //                   e4c6a70705d607e4c6a70605d608b27202730100d609b27203730200d60ab27202730300d60b
  //                   b27203730400d60cb27202730500d60db27203730600d60eb27202730700d60fb27203730800
  //                   d6108c720b02d611998c720a027210d6128c720902d613b27204730900d6149a99a37213730a
  //                   d615b27204730b00d6169d72147215d61795919e72147215730c9a7216730d7216d618b27204
  //                   730e00d6197e721805d61a9d72057219d61b998c720c028c720d02d61c998c720e028c720f02
  //                   d61d998c7208027212d1ededededed93b27202730f00b27203731000ededed93e4c672010410
  //                   720493e4c672010505720593e4c672010705720693e4c672010605720793c27201c2a7ededed
  //                   938c7208018c720901938c720a018c720b01938c720c018c720d01938c720e018c720f0193b1
  //                   72027311959172117312eded929a997205721272079c7e9995907217721872179a7218731373
  //                   1405721a937211f0721b939c7211997219a273157e721705f0721c958f72117316ededec929a
  //                   997205721272079c7e9995907217721872179a72187317731805721a92a39a9a72139c721872
  //                   15b2720473190093721bf0721192721c959172177218731a9c721b997219a2731b7e721705d8
  //                   02d61ec17201d61fc1a79590721e721fd802d620e4c672010804d6217d9d9c7e721a067e721c
  //                   067e72100605ededededed907220997217731c909972129c7e997218722005721a9a721a7207
  //                   93f0721d7221937211731d93721b731e9099721f721e7d9d9c7e7221067e7206067e72050605
  //                   edededed91721e721f937211731f93721b732093721d732193721c7322
  //
  // Validations:
  // 1. LM Pool NFT is preserved;
  // 2. LM Pool Config, LM program budget and execution budget are preserved;
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
  //    6.3. Compound: if (execBudgetRem1 < execBudgetRem0)
  //         6.3.1. Epoch is legal to perform compounding;
  //         6.3.2. Previous epoch is compounded;
  //         6.3.3. Delta reward tokens amount equals to calculated reward amount;
  //         6.3.4. Delta LQ tokens amount is 0;
  //         6.3.5. Delta vLQ tokens amount is 0;
  //         6.3.6. Execution fee amount is valid.
  //    6.4. Increase execution budget: else
  //         6.4.1. execBudgetRem1 >= execBudgetRem0;
  //         6.4.2. Delta LQ tokens amount is 0;
  //         6.4.3. Delta vLQ tokens amount is 0;
  //         6.4.4. Delta X tokens amount is 0;
  //         6.4.5. Delta TMP tokens amount is 0.
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
  val redeemLimitDelta = conf0(3)

  val programBudget0 = SELF.R5[Long].get
  val MaxRoundingError0 = SELF.R6[Long].get
  val execBudget0 = SELF.R7[Long].get

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
  val execBudget1 = successor.R7[Long].get

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
      (execBudget1 == execBudget0) &&
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

      prevEpochsCompoundedForDeposit &&
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
      val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta
      (prevEpochsCompoundedForRedeem || redeemNoLimit) &&
        // 6.2.2. & 6.2.3.
        (deltaVLQ == -deltaLQ) &&
        (deltaTMP >= minReturnedTMP)

    } else {
      val execBudgetRem0 = SELF.value
      val execBudgetRem1 = successor.value
      if (execBudgetRem1 <= execBudgetRem0) { // compound
        // 6.3.
        val epoch = successor.R8[Int].get
        val epochsToCompound = epochNum - epoch
        // 6.3.1.
        val legalEpoch = epoch <= curEpochIx - 1
        val prevEpochCompounded = (reservesX - epochsToCompound * epochAlloc) <= (epochAlloc + MaxRoundingError0)

        val reward = (epochAlloc.toBigInt * deltaTMP / reservesLQ).toLong
        val execFee = (reward.toBigInt * execBudget0 / programBudget0).toLong

        legalEpoch &&
          // 6.3.2. && 6.3.3. && 6.3.4. && 6.3.5.
          prevEpochCompounded &&
          (-deltaX == reward) &&
          (deltaLQ == 0L) &&
          (deltaVLQ == 0L) &&
          (execBudgetRem0 - execBudgetRem1) <= execFee // valid exec fee
      }
      else { // increase execution budget
        // 6.4.
        // 6.4.1. && 6.4.2. && 6.4.3. && 6.4.4. && 6.4.5.
        (execBudgetRem1 > execBudgetRem0) &&
          (deltaLQ == 0L) &&
          (deltaVLQ == 0L) &&
          (deltaX == 0L) &&
          (deltaTMP == 0L)
      }
    }
  }
  sigmaProp(nftPreserved &&
    configPreserved &&
    scriptPreserved &&
    assetsPreserved &&
    noMoreTokens &&
    validAction)
}
