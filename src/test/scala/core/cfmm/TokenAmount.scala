package io.ergodex
package core.cfmm

final case class TokenAmount(tokenId: String, amount: Long) {
  def withAmount(x: Long): TokenAmount = copy(amount = x)
}
