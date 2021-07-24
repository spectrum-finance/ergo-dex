package io.ergodex
package core.cfmm

final case class PoolConfig(feeNum: Int, feeDenom: Int, emissionLP: Long, burnLP: Long, minInitialDeposit: Long)

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
    require(inLp <= supplyLP)
    val redeemedX = inLp * x / supplyLP
    val redeemedY = inLp * y / supplyLP
    copy(x - redeemedX, y - redeemedY, lp + inLp)
  }

  def swap(asset: String, in: Long): CfmmPool = {
    require(in > 0)
    val (deltaX, deltaY) =
      if (asset == "x")
        (BigInt(in), -BigInt(y) * in * config.feeNum / (BigInt(x) * config.feeDenom + in * config.feeNum))
      else
        (-BigInt(x) * in * config.feeNum / (BigInt(y) * config.feeDenom + in * config.feeNum), BigInt(in))

    if (asset == "x")
      require((BigInt(y) * deltaX * config.feeNum) >= -deltaY * (x * config.feeDenom + deltaX * config.feeNum))
    else
      require((BigInt(x) * deltaY * config.feeNum) >= -deltaX * (y * config.feeDenom + deltaY * config.feeNum))

    val pool = copy((x + deltaX).toLong, (y + deltaY).toLong)
    require(BigInt(pool.x) * pool.y >= BigInt(x) * y, "Non-decreasing constant product doesn't hold")

    pool
  }

  /** Allows to calculate the required amount of input in token Y
    * in order to get the given output amount of token X (and vise versa)
    */
  def inputAmount(token: String, output: Long): Long =
    if (token == "x" && outputAmount("x", output) > 0)
      (y * output * config.feeDenom / ((x - output) * config.feeNum)) + 1
    else if (token == "y" && outputAmount("y", output) > 0)
      (x * output * config.feeDenom / ((y - output) * config.feeNum)) + 1
    else -1L

  /** Allows to calculate the expected amount of output in token Y
    * for the given input amount of token X (and vise versa)
    */
  def outputAmount(token: String, input: Long): Long = {
    def out(in: Long, out: Long) =
      BigInt(out) * input * config.feeNum /
      (in * config.feeDenom + input * config.feeNum)
    (if (token == "x") out(x, y) else out(y, x)).toLong
  }
}

object CfmmPool {

  def init(inX: Long, inY: Long, config: PoolConfig): CfmmPool = {
    require(inX >= config.minInitialDeposit && inY >= config.minInitialDeposit)
    val share = math.sqrt(inX * inY).toLong // todo: overflow
    CfmmPool(inX, inY, config.emissionLP - config.burnLP - share, config)
  }
}
