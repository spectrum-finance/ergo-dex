package io.ergodex
package core.cfmm

final case class PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long)

final case class CfmmPool(x: Long, y: Long, lp: Long, config: PoolConfig) {

  private val supplyLP = config.emissionLP - lp

  def deposit(inX: Long, inY: Long): CfmmPool = {
    val unlocked = math.min(
      inX * supplyLP / x,
      inY * supplyLP / y
    )
    copy(x + inX, y + inY, lp - unlocked)
  }

  def redeem(inLp: Long): CfmmPool = {
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
    val share = math.sqrt(inX * inY).toLong // todo: overflow
    CfmmPool(inX, inY, config.emissionLP - share, config)
  }
}
