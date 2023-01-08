package io.ergodex.core.sim

import io.ergodex.core.syntax.{Coll, CollOpaque}

object Helpers {
  def tokenId(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)

  def boxId(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)
}
