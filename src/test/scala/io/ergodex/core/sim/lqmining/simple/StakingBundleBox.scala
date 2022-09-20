package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.RuntimeState._
import io.ergodex.core.sim._
import io.ergodex.core.syntax._

final class StakingBundleBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val tokens: Vector[(Coll[Byte], Long)],
  override val registers: Map[Int, Any]
) extends Box[F] {
  override val validatorTag = "staking_bundle"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      val bundleId0  = SELF.tokens(0)
      val bundleVLQ0 = SELF.tokens(1)
      val bundleTMP0 = SELF.tokens(2)

      val redeemerPk0 = SELF.R4[SigmaProp].get
      val bundleKey0  = SELF.R5[Coll[Byte]].get
      val poolId0     = SELF.R6[Coll[Byte]].get

      val pool0 = INPUTS(0)
      val pool1 = OUTPUTS(0)

      val validPool = pool1.tokens(0)._1 == SELF.R6[Coll[Byte]].get

      val deltaLQ = pool1.tokens(2)._2 - pool0.tokens(2)._2

      val validAction =
        if (deltaLQ == 0L) { // compound
          val epoch               = pool1.R6[Int].get
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
          val validRedeemer       = redeemer.propositionBytes == redeemerPk0.propBytes

          val successorIndex = getVar[Int](1).get

          val successor = OUTPUTS(successorIndex)

          val bundleId1  = successor.tokens(0)
          val bundleVLQ1 = successor.tokens(1)
          val bundleTMP1 = successor.tokens(2)

          val validSuccessor =
            successor.R4[SigmaProp].get == redeemerPk0 &&
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
}
