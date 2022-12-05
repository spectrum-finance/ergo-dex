
{ // ===== Contract Information ===== //
  // Name: StakingBundle
  // Description: Contract that validates a compounding in the LM pool.
  //
  // ===== Bundle Box ===== //
  // Registers:
  //   R4[Coll[Byte]]: Redeemer Proposition  // where the reward should be sent.
  //   R5[Coll[Byte]]: Bundle Key ID (tokenId) // used to authenticate redeem.
  //   R6[Coll[Byte]]: LM Pool ID (tokenId) // used to authenticate pool.
  //
  // Tokens:
  //   0:
  //     _1: vLQ Token ID  // tokens representing locked share of LQ.
  //     _2: Amount of vLQ tokens
  //   1:
  //     _1: TMP Token ID  // left program epochs times liquidity.
  //     _2: Amount of the TMP tokens
  //
  // ContextExtension constants:
  // 0: Int - redeemer output index;
  // 1: Int - successor output index;
  // * indexes are dynamic to allow batch compounding.
  //
  //
  // ErgoTreeTemplate: d80ed601b2a5730000d602db63087201d603e4c6a7060ed604b2a4730100d605db63087204d
  //                   6068cb2720573020002d607998cb27202730300027206d608e4c6a7040ed609e4c6a7050ed6
  //                   0adb6308a7d60bb2720a730400d60cb2720a730500d60d8c720c02d60e8c720b02d1ed938cb
  //                   27202730600017203959372077307d808d60fb2a5e4e3000400d610b2a5e4e3010400d611db
  //                   63087210d612b27211730800d613b2e4c672040410730900d614c672010804d6157e9972139
  //                   5e67214e47214e4c67201070405d616b2db6308720f730a00eded93c2720f7208ededededed
  //                   93e4c67210040e720893e4c67210050e720993e4c67210060e7203938c7212018c720b01939
  //                   9720e8c72120299720e9c7215720d93b27211730b00720ced938c7216018cb27205730c0001
  //                   927e8c721602069d9c9c7e9de4c6720405057e721305067e720d067e999d720e720d7215067
  //                   e720606958f7207730d93b2db6308b2a4730e00730f008602720973107311
  //
  // Validations:
  // 1. LM Pool NFT (Token ID) is valid;
  // 2. Action is valid:
  //    2.1. Compound:
  //         2.1.1 Delta LQ tokens amount is correct;
  //         2.1.2 Delta TMP tokens amount is correct.
  //    2.2. Redeem:
  //         2.2.1 bundleKeyId matches.
  //    2.3. Compound:
  //         2.3.1 Valid redeemer;
  //         2.3.2 Valid successor.
  //
  // ===== Getting SELF data ===== //
  val bundleVLQ0 = SELF.tokens(0)
  val bundleTMP0 = SELF.tokens(1)

  val redeemerProp0 = SELF.R4[Coll[Byte]].get
  val bundleKey0 = SELF.R5[Coll[Byte]].get
  val poolId0 = SELF.R6[Coll[Byte]].get

  // ===== Getting INPUTS data ===== //
  val pool0 = INPUTS(0)
  val lqLockedInPoolTotal = pool0.tokens(2)._2

  // ===== Getting OUTPUTS data ===== //
  val pool1 = OUTPUTS(0)
  val deltaLQ = pool1.tokens(2)._2 - lqLockedInPoolTotal

  // ===== Validating conditions ===== //
  // 1.
  val validPool = pool1.tokens(0)._1 == poolId0
  // 2.
  val validAction =
    if (deltaLQ == 0L) { // compound
      // 2.1.
      // ===== Getting INPUTS data ===== //
      val conf = pool0.R4[Coll[Int]].get
      val programBudget = pool0.R5[Long].get
      val epochNum = conf(1)

      val redeemerOutIx = getVar[Int](0).get
      val successorIndex = getVar[Int](1).get

      // ===== Getting OUTPUTS data ===== //
      val redeemer = OUTPUTS(redeemerOutIx)
      val successor = OUTPUTS(successorIndex)

      val bundleVLQ1 = successor.tokens(0)
      val bundleTMP1 = successor.tokens(1)
      val redeemerRewardToken = redeemer.tokens(0)
      val epoch_ = pool1.R8[Int]
      val epoch = if (epoch_.isDefined) epoch_.get else pool1.R7[Int].get

      // ===== Getting deltas and calculate reward ===== //
      val epochsToCompound = epochNum - epoch
      val bundleVLQ = bundleVLQ0._2
      val bundleTMP = bundleTMP0._2
      val releasedTMP = bundleTMP0._2 - epochsToCompound * bundleVLQ
      val epochRewardTotal = programBudget / epochNum
      val epochsBurned = (bundleTMP / bundleVLQ) - epochsToCompound
      val reward = epochRewardTotal.toBigInt * bundleVLQ * epochsBurned / lqLockedInPoolTotal
      // ===== Validating conditions ===== //
      // 2.1.1.
      val validRedeemer = redeemer.propositionBytes == redeemerProp0
      // PubKey matches
      // PubKey matches; bundleKeyID; LM Pool ID; TMP token ID;  TMP token amount;
      // 2.1.2.
      val validSuccessor =
      (successor.R4[Coll[Byte]].get == redeemerProp0) &&
        (successor.R5[Coll[Byte]].get == bundleKey0) &&
        (successor.R6[Coll[Byte]].get == poolId0) &&
        (bundleTMP1._1 == bundleTMP0._1) &&
        ((bundleTMP - bundleTMP1._2) == releasedTMP) &&
        (bundleVLQ1 == bundleVLQ0)
      // 2.1.3.
      val validReward =
        (redeemerRewardToken._1 == pool0.tokens(1)._1) &&
          (redeemerRewardToken._2 >= reward)

      validRedeemer &&
        validSuccessor &&
        validReward

    } else if (deltaLQ < 0L) { // redeem (validated by redeem order)
      // 2.2.
      // ===== Getting INPUTS data ===== //
      val permitIn = INPUTS(2)
      val requiredPermit = (bundleKey0, 0x7fffffffffffffffL)
      // ===== Validating conditions ===== //
      // 2.2.1.
      permitIn.tokens(0) == requiredPermit

    } else {
      false
    }

  sigmaProp(validPool &&
    validAction)
}
