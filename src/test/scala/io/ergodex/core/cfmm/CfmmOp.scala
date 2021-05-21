package io.ergodex.core.cfmm

sealed trait CfmmOp

final case class Deposit(x: Long, y: Long) extends CfmmOp
final case class Redeem(lp: Long) extends CfmmOp
final case class Swap(asset: String, amount: Long) extends CfmmOp
