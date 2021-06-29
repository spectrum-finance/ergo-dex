package io.ergodex
package core.cfmm2

/**  @param feeNum is the numerator in the ratio of fee to be taken.
  *  @param denom is the denominator in the ratio of fee to be taken.
  *
  *  For example, if fee is 5% then set feeNum = 5 and denom = 100
  */
final case class Fee(feeNum: Int, denom: Int) {
  require(feeNum > 0) // 0% fee is not permitted
  require(feeNum < denom) // 100% fee is not permitted

  def this(feeNum: Int) = this(feeNum, 1000) // default denom is 1000
  val nonFeeNum = denom - feeNum // The numerator of the remaining part after fee is deducted
}

/**  @param x the current reserves of token X in the pool
  *  @param y the current reserves of token Y in the pool
  *  @param supplyLP the current supply of LP tokens (i.e., the LP tokens in circulation)
  *  @param fee the fee fraction defined using num/denom
  *
  *  Primary constructor is private and cannot be directly called.
  */
final case class CfmmPool private (x: Long, y: Long, supplyLP: Long, fee: Fee) {
  import fee._

  require(x >= 0, s"X must be > 0. Currently $x")
  require(y >= 0, s"Y must be > 0. Currently $y")
  require(supplyLP >= 0, s"LP must be > 0. Currently $supplyLP")

  /** The secondary constructor initializes the pool using some initial deposit of X and Y, and releases LP tokens based on the formula LP = Sqrt(X, Y)
    * @param inX initial X deposited
    * @param inY initial X deposited
    * @param fee fee config to use throughout the lifecycle of this object and its clones
    */
  def this(inX: Long, inY: Long, fee: Fee) = this(inX, inY, math.sqrt(inX * inY).toLong, fee) // todo: overflow

  def this(inX: Long, inY: Long) = this(inX, inY, new Fee(3)) // default fee is 0.3 percent

  /** Used for adding liquidity to the pool.
    * @param dX amount of X tokens deposited (at least 1)
    * @param dY amount of Y tokens deposited (at least 1)
    * @return the number of LP tokens rewarded. Also, supplyLP is increased by the same amount
    */
  def deposit(dX: Long, dY: Long): Option[CfmmPool] = {
    require(dX > 0 && dY > 0)
    val unlockedLP = math.min(
      dX * supplyLP / x,
      dY * supplyLP / y
    )
    if (unlockedLP > 0) Some(copy(x + dX, y + dY, supplyLP + unlockedLP)) else None
  }

  def redeem(dLp: Long): Option[CfmmPool] = {
    require(dLp > 0)
    val redeemedX = dLp * x / supplyLP
    val redeemedY = dLp * y / supplyLP
    require(redeemedX >= 0)
    require(redeemedY >= 0)
    if (redeemedX > 0 || redeemedY > 0) Some(copy(x - redeemedX, y - redeemedY, supplyLP - dLp)) else None
  }

  /** Swap X/Y by selling X or Y (and buying the other), taking fee into account
    * @param reserveSell the pool's reserve of tokens being sold
    * @param reserveBuy the pool's reserve of tokens being purchased
    * @param deltaSell the delta of tokens being sold (positive)
    * @return the delta of tokens purchased (negative) as Option[Long] if values are within range, else None
    */
  private def getDeltaBuy(reserveSell: Long, reserveBuy: Long, deltaSell: Long): Option[Long] = {
    require(deltaSell > 0, s"deltaSell must be > 0. Currently $deltaSell")
    val deltaBuy = (-BigInt(reserveBuy) * deltaSell * nonFeeNum / (reserveSell * denom + deltaSell * nonFeeNum)).toLong
    require(
      BigInt(reserveBuy) * deltaSell * nonFeeNum >= -BigInt(deltaBuy) * (reserveSell * denom + deltaSell * nonFeeNum)
    )
    require(
      BigInt(reserveSell + deltaSell) * BigInt(reserveBuy + deltaBuy) > BigInt(reserveSell) * BigInt(reserveBuy),
      "Non-decreasing constant product doesn't hold"
    )

    if (deltaBuy < 0) {
      if (deltaBuy + reserveBuy > 0) Some(deltaBuy)
      else {
        println(
          s"|deltaBuy| must be < reservesBuy. Currently deltaBuy = $deltaBuy and reservesBuy = $reserveBuy"
        ) // todo: logging
        None
      }
    } else {
      println(s"[debug] deltaBuy should be < 0. Currently $deltaBuy") // todo: logging
      None
    }
  }

  def sellX(deltaX: Long): Option[CfmmPool] = getDeltaBuy(x, y, deltaX).map(deltaY => copy(x + deltaX, y + deltaY))
  def sellY(deltaY: Long): Option[CfmmPool] = getDeltaBuy(y, x, deltaY).map(deltaX => copy(x + deltaX, y + deltaY))

  def donateXY(dX: Long, dY: Long): CfmmPool = {
    require(dX >= 0, s"dX must be >= 0. Currently $dX")
    require(dY >= 0, s"dX must be >= 0. Currently $dY")
    copy(x + dX, y + dY, supplyLP).ensuring(dX + dY > 1, "At least one of dX or dY must be > 0")
  }

  // helper methods
  /** This answers the question "How much Y will I get when I sell soldX amount of X?"
    * @param soldX tokens X sold (added to pool)
    * @return delta of tokens Y purchased (reduced from pool)
    */
  def getBoughtY(soldX: Long) = getDeltaBuy(x, y, soldX).map(_ * -1)

  /** This answers the question "How much X will I get when I sell soldY amount of Y?"
    * @param soldY tokens Y sold (added to pool)
    * @return delta of tokens X purchased (reduced from pool)
    */
  def getBoughtX(soldY: Long) = getDeltaBuy(y, x, soldY).map(_ * -1)

  // reverse of getDeltaBuy
  private def getDeltaSell(reservesSell: Long, reservesBuy: Long, deltaBuy: Long): Option[Long] = {
    require(deltaBuy < reservesBuy)
    val deltaSell = (reservesSell * denom * deltaBuy) / (nonFeeNum * (reservesBuy - deltaBuy))
    if (deltaSell > 0) Some(deltaSell + 1) else None
  }

  /** This answers the question "How much X will I have to sell to get boughtY amount of tokens Y?"
    * @param boughtY amount to tokens Y to buy
    * @return the amount of tokens to sell
    */
  def getSoldX(boughtY: Long): Option[Long] =
    for {
      _     <- getBoughtX(boughtY)
      soldX <- getDeltaSell(x, y, boughtY)
    } yield soldX

  /** This answers the question "How much Y will I have to sell to get boughtX amount of tokens X?"
    * @param boughtX amount to tokens X to buy
    * @return the amount of tokens to sell
    */
  def getSoldY(boughtX: Long): Option[Long] =
    for {
      _     <- getBoughtY(boughtX)
      soldY <- getDeltaSell(y, x, boughtX)
    } yield soldY

  /** Allows to calculate the required amount of input in token Y
    * in order to get the given output amount of token X (and vise versa)
    */
  @deprecated("Kept for testing")
  def inputAmount(token: String, output: Long): Long =
    if (token == "x" && outputAmount("x", output) > 0)
      (y * output * denom / ((x - output) * nonFeeNum)) + 1
    else if (token == "y" && outputAmount("y", output) > 0)
      (x * output * denom / ((y - output) * nonFeeNum)) + 1
    else -1L

  /** Allows to calculate the expected amount of output in token Y
    * for the given input amount of token X (and vise versa)
    */
  @deprecated("Kept for testing")
  def outputAmount(token: String, input: Long): Long = {
    def out(in: Long, out: Long) =
      BigInt(out) * input * nonFeeNum /
      (in * denom + input * nonFeeNum)
    (if (token == "x") out(x, y) else out(y, x)).toLong
  }
}
