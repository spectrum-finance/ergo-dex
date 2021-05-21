package io.ergodex
package core.cfmm

final case class PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, minInitialDeposit: Long)

final case class CfmmPool(x: Long, y: Long, lp: Long, config: PoolConfig) {

  val supplyLP: Long = config.emissionLP - lp

  def deposit(inX: Long, inY: Long): CfmmPool = {
    val unlocked = math.min(
      inX * supplyLP / x,
      inY * supplyLP / y
    )
    copy(x + inX, y + inY, lp - unlocked)
  }

  def redeem(inLp: Long): CfmmPool = {
    require(inLp <= supplyLP, "Illegal LP amount")
    val redeemedX = inLp * x / supplyLP
    val redeemedY = inLp * y / supplyLP
    copy(x - redeemedX, y - redeemedY, lp + inLp)
  }

  def swap(asset: String, in: Long): CfmmPool = {
    val (deltaX, deltaY) =
      if (asset == "x")
        (in, -y * in * config.feeNum / (x * config.feeDenom + in * config.feeNum))
      else
        (-x * in * config.feeNum / (y * config.feeDenom + in * config.feeNum), in)
    copy(x + deltaX, y + deltaY)
  }
}

object CfmmPool {

  def init(inX: Long, inY: Long, config: PoolConfig): CfmmPool = {
    //require(inX >= config.minInitialDeposit && inY >= config.minInitialDeposit, s"Min deposit not satisfied {$inX|${inY}")
    val share = math.sqrt(inX * inY).toLong // todo: overflow
    CfmmPool(inX, inY, config.emissionLP - share, config)
  }
}
