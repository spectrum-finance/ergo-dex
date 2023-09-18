package io.ergodex.core.lqmining.parallel

import io.ergodex.core.Helpers.hex
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{BoxSim, RuntimeState}

final class UserBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val validatorBytes: String = hex("user")
) extends BoxSim[F] {

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      true
    }
}
