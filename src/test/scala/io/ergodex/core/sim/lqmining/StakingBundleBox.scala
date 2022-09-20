package io.ergodex.core.sim.lqmining

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
      {
        val bundleKey0 = SELF.tokens(0)
        val bundleVLQ0 = SELF.tokens(1)
        val bundleTMP0 = SELF.tokens(2)

        val redeemer0 = SELF.R4[SigmaProp]

        val successorIndex = getVar[Int](0).get

        val successor = OUTPUTS(successorIndex)

        val bundleKey1 = successor.tokens(0)
        val bundleVLQ1 = successor.tokens(1)
        val bundleTMP1 = successor.tokens(2)

        val redeemer1 = successor.R4[SigmaProp]

        val pool0 = INPUTS(0)
        val pool1 = OUTPUTS(0)

        val deltaLQ = pool1.tokens(2)._2 - pool0.tokens(2)._2

        val validAction =
          if (deltaLQ == 0L) { // compound
            val epoch            = pool1.R9[Int].get
            val conf             = pool0.R4[Coll[Int]].get
            val epochLen         = conf(1)
            val epochNum         = conf(2)
            val epochsToCompound = epochNum - epoch
            val bundleVLQ        = bundleVLQ0._2
            val bundleTMP        = bundleTMP0._2
            val framesBurned     = (bundleTMP / bundleVLQ) - epochLen * epochsToCompound
            val revokedTMP       = framesBurned * bundleVLQ

            bundleTMP1._1 == bundleTMP0._1 &&
            (bundleTMP - bundleTMP1._2) == revokedTMP &&
            bundleKey1 == bundleKey0 &&
            bundleVLQ1 == bundleVLQ0
          } else if (deltaLQ < 0L) { // redeem
            false
          } else {
            false
          }

        validAction
      }
    }
}
