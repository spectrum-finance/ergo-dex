package io.ergodex.core.cfmm3

import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{Box, RuntimeState}

final class UserBox[F[_] : RuntimeState](
                                          override val id: Coll[Byte],
                                          override val value: Long,
                                          override val creationHeight: Int,
                                          override val tokens: Vector[(Coll[Byte], Long)],
                                          override val registers: Map[Int, Any]
                                        ) extends Box[F] {
  override val validatorTag = "redeemer"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      true
    }

}