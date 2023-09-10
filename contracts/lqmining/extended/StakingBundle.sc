{ // ===== Contract Information ===== //
  // Name: StakingBundle (extended LM)
  // Description: Contract that validates a compounding in the extended LM pool.
  //
  // ===== Bundle Box ===== //
  // Registers:
  //   R4[Coll[Byte]]: BundleKeyToken name.
  //   R5[Coll[Byte]]: BundleKeyToken info.
  //   R6[SigmaProp]: Redeemer Sigma Proposition.  // address which can claim a rewards.
  //   R7[Coll[Byte]]: LM Pool ID (tokenId).  // used to authenticate pool.
  //
  // Constants:
  //  {1} -> actionId[Int]
  //
  // Tokens:
  //   0:
  //     _1: vLQ Token ID  // tokens representing locked share of LQ.
  //     _2: Amount of vLQ tokens.
  //   1:
  //     _1: TMP Token ID  // left program epochs times liquidity.
  //     _2: Amount of the TMP tokens.
  //   2:
  //     _1: BundleKeyId // BundleKeyToken.
  //     _2: 1L
  //   3:
  //     _1: Main reward Token ID.
  //     _2: Amount of main reward tokens.
  //   4:
  //     _1: Optional reward Token ID.
  //     _2: Amount of optional reward tokens.
  //
  // ContextExtension constants:
  // 0: Int - redeemer output index;
  // 1: Int - successor output index;
  // 2: Int - redeemed reward token index;
  // * indexes are dynamic to allow batch compounding.
  //
  // ErgoTree: 198c0737040004160400040004000404040404020408040a0400050004020502040205feffffffffffffffff010400050004020404040604020404050005020500050005020500040a0502050205000101010004000502050204000500040404020400040405fcffffffffffffffff010100040204000400040205fcffffffffffffffff01040405fcffffffffffffffff0105000502d804d601e4c6a7070ed602db6308a7d603b27202730000d604e4c6a70608d1959373017302d80bd605b2a5730300d606db63087205d607b2a4730400d608db63087207d6098cb2720873050002d60a998cb27206730600027209d60bb27208730700d60cb27208730800d60db27208730900d60e8c720b01d60f8c720d01ed938cb27206730a000172019593720a730bd814d610e4c672070511d611c672050804d61299b2e4c672070410730c0095e67211e47211e4c672050704d6137e721205d614997213730dd615b27202730e00d6168c721502d6179972169c72138c720302d6189999730f8c720c029c72097213d6199d9c998c720b029cb27210731000721472177218d61a9172197311d61b9d9c998c720d029cb27210731200721472177218d61cb2a5e4e3010400d61ddb6308721cd61eb2721d731300d61fb2721d731400d620b2721d731500d6218c722001d6228c722002d6238cb2720273160001ededededed95ed721a93721b7317ed938c721e01720e928c721e029972197318d801d624937219731995ed722491721b731aed938c721f01720f928c721f0299721b731b95ed721a91721b731cd801d625b2721d731d00ededed937221720e927222997219731e938c722501720f928c72250299721b731f95ed722493721b73207321732293e4c6721c0608720493e4c6721c070e720193c2721cc2a7959172127323eded93860272237324721e9372218c721501939972167222721793860272237325722093b2721d7326007203958f720a7327d802d610b2a4732800d6118cb2720273290001ed93b2db63087210732a008602959472118c720c018cb27202732b00017211732c93c27210d07204732dd805d605b2a4732e00d606b2db63087205732f00d607b2a5733000d608e4e30204d6098cb2720272080002ec93720686028cb27202733100017332edededededed93720686028cb2720273330001733493c27205d072048f998cb2db630872077208000272097335927209733693e4c672070608720493e4c67207070e720193c27207c2a7
  //
  // ErgoTreeHash:
  //
  // ErgoTreeTemplate: d804d601e4c6a7070ed602db6308a7d603b27202730000d604e4c6a70608d1959373017302d80bd605b2a5730300d606db63087205d607b2a4730400d608db63087207d6098cb2720873050002d60a998cb27206730600027209d60bb27208730700d60cb27208730800d60db27208730900d60e8c720b01d60f8c720d01ed938cb27206730a000172019593720a730bd814d610e4c672070511d611c672050804d61299b2e4c672070410730c0095e67211e47211e4c672050704d6137e721205d614997213730dd615b27202730e00d6168c721502d6179972169c72138c720302d6189999730f8c720c029c72097213d6199d9c998c720b029cb27210731000721472177218d61a9172197311d61b9d9c998c720d029cb27210731200721472177218d61cb2a5e4e3010400d61ddb6308721cd61eb2721d731300d61fb2721d731400d620b2721d731500d6218c722001d6228c722002d6238cb2720273160001ededededed95ed721a93721b7317ed938c721e01720e928c721e029972197318d801d624937219731995ed722491721b731aed938c721f01720f928c721f0299721b731b95ed721a91721b731cd801d625b2721d731d00ededed937221720e927222997219731e938c722501720f928c72250299721b731f95ed722493721b73207321732293e4c6721c0608720493e4c6721c070e720193c2721cc2a7959172127323eded93860272237324721e9372218c721501939972167222721793860272237325722093b2721d7326007203958f720a7327d802d610b2a4732800d6118cb2720273290001ed93b2db63087210732a008602959472118c720c018cb27202732b00017211732c93c27210d07204732dd805d605b2a4732e00d606b2db63087205732f00d607b2a5733000d608e4e30204d6098cb2720272080002ec93720686028cb27202733100017332edededededed93720686028cb2720273330001733493c27205d072048f998cb2db630872077208000272097335927209733693e4c672070608720493e4c67207070e720193c27207c2a7
  //
  // ErgoTreeTemplateHash: f213f4bcba7a83cc0153fffbd27388e0ea2b81ef090c6da1120e18125766f7c1
  //
  // ===== Getting SELF data ===== //
  val bundleVLQ0 = SELF.tokens(0)

  val redeemerProp0 = SELF.R6[SigmaProp].get
  val poolId0       = SELF.R7[Coll[Byte]].get

  // ===== Getting INPUTS data ===== //
  val validOperation = {
    if (actionId == 0) {
      val pool0            = INPUTS(0)
      val poolMainReward0  = pool0.tokens(1)._2
      val poolReservesLQ0  = pool0.tokens(2)._2
      val poolTMP0         = pool0.tokens(4)
      val poolReservesTMP0 = poolTMP0._2
      val poolOptReward0   = pool0.tokens(5)._2

      // ===== Getting OUTPUTS data ===== //
      val pool1   = OUTPUTS(0)
      val deltaLQ = pool1.tokens(2)._2 - poolReservesLQ0

      // ===== Validating conditions ===== //
      // 1.
      val validPool = pool1.tokens(0)._1 == poolId0
      // 2.
      val validAction =
        if (deltaLQ == 0L) { // compound
          // 2.1.
          // ===== Getting SELF data ===== //
          val bundleTMP0  = SELF.tokens(1)
          val bundleKey0  = SELF.tokens(2)._1
          val mainReward0 = SELF.tokens(3)
          val optReward0  = SELF.tokens(4)

          // ===== Getting INPUTS data ===== //
          val conf                   = pool0.R4[Coll[Int]].get
          val budgets0               = pool0.R5[Coll[Long]].get
          val prevMainProgramBudget0 = budgets0(0)
          val prevOptProgramBudget0  = budgets0(1)
          val epochNum               = conf(1)

          val successorIndex = getVar[Int](1).get

          // ===== Getting OUTPUTS data ===== //
          val successor   = OUTPUTS(successorIndex)
          val mainReward1 = successor.tokens(3)
          val optReward1  = successor.tokens(4)

          val bundleVLQ1 = successor.tokens(0)
          val epoch_     = pool1.R8[Int]
          val epoch      = if (epoch_.isDefined) epoch_.get else pool1.R7[Int].get

          // ===== Getting deltas and calculate reward ===== //
          val epochsToCompound = epochNum - epoch
          val bundleVLQ        = bundleVLQ0._2
          val bundleTMP        = bundleTMP0._2
          val releasedTMP      = bundleTMP0._2 - epochsToCompound * bundleVLQ
          val mainRewardDelta  = mainReward1._2 - mainReward0._2
          val optRewardDelta   = optReward1._2 - optReward0._2

          val actualTMP    = 0x7fffffffffffffffL - poolReservesTMP0 - poolReservesLQ0 * epochsToCompound
          val allocMainRem = poolMainReward0 - prevMainProgramBudget0 * (epochsToCompound - 1L)
          val allocOptRem  = poolOptReward0 - prevOptProgramBudget0 * (epochsToCompound - 1L)

          val rewardMain = allocMainRem * releasedTMP / actualTMP
          val rewardOpt  = allocOptRem * releasedTMP / actualTMP

          // ===== Validating conditions ===== //
          val validTMPAndKey = if (epochsToCompound > 0) {
            val bundleTMP1 = successor.tokens(1)
            (bundleKey0, 1L) == successor.tokens(2) &&
            (bundleTMP1._1 == bundleTMP0._1) &&
            (bundleTMP - bundleTMP1._2 == releasedTMP)
          } else {
            (bundleKey0, 1L) == successor.tokens(1)
          }
          // 2.1.1.
          val validReward = if (rewardMain > 0 && rewardOpt == 0) {
            val redeemerRewardToken = successor.tokens(2)

            (redeemerRewardToken._1 == pool0.tokens(1)._1) &&
            (redeemerRewardToken._2 >= rewardMain - 1L)

          } else if (rewardMain == 0 && rewardOpt > 0) {
            val redeemerRewardToken = successor.tokens(3)

            (redeemerRewardToken._1 == pool0.tokens(5)._1) &&
            (redeemerRewardToken._2 >= rewardOpt - 1L)

          } else if (rewardMain > 0 && rewardOpt > 0) {
            val redeemerRewardMainToken = successor.tokens(1)
            val redeemerRewardOptToken  = successor.tokens(5)

            (redeemerRewardMainToken._1 == pool0.tokens(1)._1) &&
            (redeemerRewardMainToken._2 >= rewardMain - 1L) &&
            (redeemerRewardOptToken._1 == pool0.tokens(5)._1) &&
            (redeemerRewardOptToken._2 >= rewardOpt - 1L)

          } else if (rewardMain == 0 && rewardOpt == 0) { true }
          else false

          validReward &&
          (successor.R6[SigmaProp].get == redeemerProp0) &&
          (successor.R7[Coll[Byte]].get == poolId0) &&
          (successor.propositionBytes == SELF.propositionBytes) &&
          validTMPAndKey &&
          (bundleVLQ1 == bundleVLQ0)

        } else if (deltaLQ < 0L) { // redeem
          // 2.2.
          // ===== Getting SELF data ===== //
          val bundleKey0 = {
            if (SELF.tokens(1)._1 != poolTMP0._1) {
              SELF.tokens(2)._1
            } else SELF.tokens(1)._1
          }

          // ===== Getting INPUTS data ===== //
          val permitIn       = INPUTS(2)
          val requiredPermit = (bundleKey0, 0x7fffffffffffffffL - 1L)

          // ===== Validating conditions ===== //
          // 2.2.1.
          (permitIn.tokens(0) == requiredPermit) &&
          (permitIn.propositionBytes == redeemerProp0.propBytes)
        } else {
          false
        }

      validPool &&
      validAction

    } else  // redeem rewards
    {

      val permitIn                = INPUTS(1)
      val requiredPermitPossible0 = (SELF.tokens(1)._1, 0x7fffffffffffffffL - 1L)
      val requiredPermitPossible1 = (SELF.tokens(2)._1, 0x7fffffffffffffffL - 1L)

      val rewardInd     = getVar[Int](2).get
      val bundleReward0 = SELF.tokens(rewardInd)

      val bundleOut     = OUTPUTS(0)
      val bundleReward1 = bundleOut.tokens(rewardInd)

      val bundleRewardDelta = bundleReward1._2 - bundleReward0._2

      (permitIn.tokens(0) == requiredPermitPossible0) || (permitIn.tokens(0) == requiredPermitPossible1) &&
      (permitIn.propositionBytes == redeemerProp0.propBytes) &&
      (bundleRewardDelta < 0) &&
      (bundleReward0._2 >= 1) &&
      (bundleOut.R6[SigmaProp].get == redeemerProp0) &&
      (bundleOut.R7[Coll[Byte]].get == poolId0) &&
      (bundleOut.propositionBytes == SELF.propositionBytes)

    }
  }
  sigmaProp(validOperation)
}
