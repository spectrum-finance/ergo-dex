package io.ergodex.core.sim

import io.ergodex.core.syntax.Coll

object Helpers {

  def tokenId(s: String): Coll[Byte] = s.getBytes().toVector
  def boxId(s: String): Coll[Byte]   = s.getBytes().toVector
}
