{ // ===== Contract Information ===== //
  // Name: StakingBundle
  // Description: Contract that validates a compounding in the LM pool.

  // ===== Bundle Box ===== //
  // Registers:
  //   R4[SigmaProp]: Redeemer Proposition  // where the reward should be sent.
  //   R5[Coll[Byte]]: Bundle Key ID (tokenId) // used to authenticate redeem.
  //   R6[Coll[Byte]]: LM Pool ID (tokenId) // used to authenticate pool.
  //
  // Tokens:
  //   0:
  //     _1: Bundle ID
  //     _2: Amount: 0x7fffffffffffffffL
  //   1:
  //     _1: vLQ Token ID  // tokens representing locked share of LQ.
  //     _2: Amount of vLQ tokens
  //   2:
  //     _1: TMP Token ID  // left program epochs times liquidity.
  //     _2: Amount of the TMP tokens
  //
  // ContextExtension constants:
  // 0: Int - redeemer output index;
  // 1: Int - successor output index;
  // * indexes are dynamic to allow batch compounding.
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
  val bundleId0 = SELF.tokens(0)
  val bundleVLQ0 = SELF.tokens(1)
  val bundleTMP0 = SELF.tokens(2)

  val redeemerProp0 = SELF.R4[SigmaProp].get
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

      val bundleId1 = successor.tokens(0)
      val bundleVLQ1 = successor.tokens(1)
      val bundleTMP1 = successor.tokens(2)
      val redeemerRewardToken = redeemer.tokens(2)
      val epoch = pool1.R7[Int].get

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
      val validRedeemer = redeemer.propositionBytes == redeemerProp0.propBytes
      // 2.1.2.
      val validSuccessor =
      (successor.R4[SigmaProp].get.propBytes == redeemerProp0.propBytes) &&
        (successor.R5[Coll[Byte]].get == bundleKey0) &&
        (successor.R6[Coll[Byte]].get == poolId0) &&
        (bundleTMP1._1 == bundleTMP0._1) &&
        ((bundleTMP - bundleTMP1._2) == releasedTMP) &&
        (bundleId1 == bundleId0) &&
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
