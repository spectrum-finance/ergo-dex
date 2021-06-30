package io.ergodex.core.cfmm2

sealed trait CfmmOp

final case class Deposit(x: Long, y: Long) extends CfmmOp
final case class Redeem(lp: Long) extends CfmmOp
final case class SellX(amount: Long) extends CfmmOp
final case class SellY(amount: Long) extends CfmmOp
