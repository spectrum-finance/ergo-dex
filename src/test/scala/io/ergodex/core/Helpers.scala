package io.ergodex.core

import io.ergodex.core.syntax.{Coll, CollOpaque}
import scorex.util.encode.Base16

object Helpers {
  def tokenId(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)

  def boxId(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)

  def bytes(s: String): Coll[Byte] = CollOpaque(s.getBytes().toVector)

  def hex(s: String): String = Base16.encode(s.getBytes())
}
