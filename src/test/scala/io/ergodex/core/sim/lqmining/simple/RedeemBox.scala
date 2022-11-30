package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{Box, RuntimeState, SigmaProp}
import io.ergodex.core.syntax._

final class RedeemBox[F[_] : RuntimeState](
                                            override val id: Coll[Byte],
                                            override val value: Long,
                                            override val tokens: Vector[(Coll[Byte], Long)],
                                            override val registers: Map[Int, Any]
                                          ) extends Box[F] {
  override val validatorTag = "redeem_order"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Context (declarations here are only for simulations):
      val RedeemerPk = SigmaProp("user")
      val ExpectedLQ = SELF.tokens(0)._1
      val ExpectedLQAmount = SELF.tokens(0)._2

      // ===== Contract Information ===== //
      // Name: Redeem
      // Description: Contract that validates user's redeem from the LM Pool.
      //
      // Validations:
      // 1. Redeemer out is valid: Redeemer PubKey matches PubKey in Bundle Box; vLQ token ID; vLQ token amount; bundle key ID.
      //
      // ===== Getting OUTPUTS data ===== //
      val redeemerOut = OUTPUTS(1)

      // ===== Validating conditions ===== //
      // 1.
      val validRedeemerOut = {
        (redeemerOut.propositionBytes == RedeemerPk.propBytes) &&
          ((ExpectedLQ, ExpectedLQAmount) == redeemerOut.tokens(0))
      }
      RedeemerPk || validRedeemerOut
    }
}
