package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{BoxSim, RuntimeState}
import io.ergodex.core.syntax._

final class UserBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any]
) extends BoxSim[F] {
  override val validatorBytes = "user"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      true
    }
}
