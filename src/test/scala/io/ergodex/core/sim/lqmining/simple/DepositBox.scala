package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{Box, RuntimeState, SigmaProp}
import io.ergodex.core.syntax._

final class DepositBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val tokens: Vector[(Coll[Byte], Long)],
  override val registers: Map[Int, Any]
) extends Box[F] {
  override val validatorTag = "deposit_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      val expectedNumEpochs = SELF.R5[Int].get
      val poolId            = SELF.R6[Coll[Byte]].get
      val redeemerPk        = SELF.R4[SigmaProp].get

      val redeemerOut = OUTPUTS(1) // 0 -> pool_out, 1 -> redeemer_out, 2 -> bundle_out
      val bundleOut   = OUTPUTS(2)

      val poolIn      = INPUTS(0)
      val bundleKeyId = poolIn.id

      val deposit     = SELF.tokens(0)
      val expectedVLQ = deposit._2
      val expectedTMP = expectedVLQ * expectedNumEpochs

      val validPoolIn = poolIn.tokens(0)._1 == poolId
      val validRedeemerOut =
        redeemerOut.propositionBytes == redeemerPk.propBytes &&
        (bundleKeyId, 0x7fffffffffffffffL) == redeemerOut.tokens(0)

      val validBundle =
        bundleOut.R4[SigmaProp].get.propBytes == redeemerPk.propBytes &&
        bundleOut.R5[Coll[Byte]].get == bundleKeyId &&
        (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(1) &&
        (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(2)

      redeemerPk || (validPoolIn && validRedeemerOut && validBundle)
    }

}
