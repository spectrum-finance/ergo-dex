package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{Box, RuntimeState, SigmaProp}
import io.ergodex.core.syntax._

final class RedeemBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val tokens: Vector[(Coll[Byte], Long)],
  override val registers: Map[Int, Any]
) extends Box[F] {
  override val validatorTag = "redeem_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      val expectedLQ       = SELF.R4[Coll[Byte]].get
      val expectedLQAmount = SELF.R5[Long].get
      val redeemerPk       = SELF.R6[SigmaProp].get

      val redeemerOut = OUTPUTS(1)

      val validRedeemerOut =
        redeemerOut.propositionBytes == redeemerPk.propBytes &&
        (expectedLQ, expectedLQAmount) == redeemerOut.tokens(0)

      redeemerPk || validRedeemerOut
    }

}
