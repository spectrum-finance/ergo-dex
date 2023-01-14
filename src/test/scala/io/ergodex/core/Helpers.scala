package io.ergodex.core

import io.ergodex.core.syntax.{Coll, CollOpaque}

object Helpers {
  def bytes(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)

  def boxId(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)
}
