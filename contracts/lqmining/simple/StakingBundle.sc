// Pool box registers mapping:
// R4: Coll[Byte] - redeemer proposition; where the reward should be sent.
// R5: Long       - bundle key ID (tokenId); used to authenticate redeem.
// R6: Long       - pool ID (tokenId)   ; used to authenticate pool.
// 
// ContextExtension constants:
// 0: Int - redeemer output index
// 1: Int - sucessor output index
// * indexes are dynamic to allow batch compounding.
{
  val bundleId0  = SELF.tokens(0)
  val bundleVLQ0 = SELF.tokens(1)
  val bundleTMP0 = SELF.tokens(2)

  val redeemerProp0 = SELF.R4[Coll[Byte]].get
  val bundleKey0    = SELF.R5[Coll[Byte]].get
  val poolId0       = SELF.R6[Coll[Byte]].get

  val pool0 = INPUTS(0)
  val pool1 = OUTPUTS(0)

  val validPool = pool1.tokens(0)._1 == SELF.R6[Coll[Byte]].get

  val deltaLQ = pool1.tokens(2)._2 - pool0.tokens(2)._2

  val validAction =
    if (deltaLQ == 0L) { // compound
      val epoch               = pool1.R7[Int].get
      val conf                = pool0.R4[Coll[Int]].get
      val epochNum            = conf(2)
      val programBudget       = pool0.R5[Long].get
      val lqLockedInPoolTotal = pool0.tokens(2)._2
      val epochRewardTotal    = programBudget / epochNum

      val epochsToCompound = epochNum - epoch
      val bundleVLQ        = bundleVLQ0._2
      val bundleTMP        = bundleTMP0._2
      val releasedTMP      = bundleTMP - epochsToCompound * bundleVLQ

      val redeemerOutIx       = getVar[Int](0).get
      val redeemer            = OUTPUTS(redeemerOutIx)
      val redeemerRewardToken = redeemer.tokens(0)
      val validRedeemer       = redeemer.propositionBytes == redeemerProp0

      val successorIndex = getVar[Int](1).get

      val successor = OUTPUTS(successorIndex)

      val bundleId1  = successor.tokens(0)
      val bundleVLQ1 = successor.tokens(1)
      val bundleTMP1 = successor.tokens(2)

      val validSuccessor =
        successor.R4[Coll[Byte]].get == redeemerProp0 &&
        successor.R5[Coll[Byte]].get == bundleKey0 &&
        successor.R6[Coll[Byte]].get == poolId0 &&
        bundleTMP1._1 == bundleTMP0._1 &&
        (bundleTMP - bundleTMP1._2) == releasedTMP &&
        bundleId1 == bundleId0 &&
        bundleVLQ1 == bundleVLQ0

      val reward = epochRewardTotal.toBigInt * bundleVLQ / lqLockedInPoolTotal
      val validReward =
        redeemerRewardToken._1 == pool0.tokens(1)._1 &&
        redeemerRewardToken._2 == reward

      validRedeemer &&
      validSuccessor &&
      validReward
    } else if (deltaLQ < 0L) { // redeem (validated by redeem order)
      val permitIn       = INPUTS(2)
      val requiredPermit = (bundleKey0, 0x7fffffffffffffffL)

      permitIn.tokens(0) == requiredPermit
    } else {
      false
    }

  validPool &&
  validAction
}
