{
  val poolNFT0 = SELF.tokens(0)
  val poolX0   = SELF.tokens(1)
  val poolLQ0  = SELF.tokens(2)
  val poolVLQ0 = SELF.tokens(3)
  val poolTMP0 = SELF.tokens(4)

  val conf0 = SELF.R4[Coll[Int]].get

  val epochLen     = conf0(0)
  val epochNum     = conf0(1)
  val programStart = conf0(2)

  val programBudget = SELF.R5[Long].get
  val epochAlloc    = programBudget / epochNum

  val successor = OUTPUTS(0)

  val poolNFT1 = successor.tokens(0)
  val poolX1   = successor.tokens(1)
  val poolLQ1  = successor.tokens(2)
  val poolVLQ1 = successor.tokens(3)
  val poolTMP1 = successor.tokens(4)

  val conf1          = successor.R4[Coll[Int]].get
  val programBudget1 = successor.R5[Long].get

  val nftPreserved    = poolNFT1 == poolNFT0
  val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
  val configPreserved = conf1 == conf0 && programBudget1 == programBudget

  val assetsPreserved =
    poolX1._1 == poolX0._1 &&
    poolLQ1._1 == poolLQ0._1 &&
    poolVLQ1._1 == poolVLQ0._1 &&
    poolTMP1._1 == poolTMP0._1

  // since tokens can be repeated, we ensure for sanity that there are no more tokens
  val noMoreTokens = successor.tokens.size == 5

  val validCollateral = successor.value >= SELF.value

  val reservesX  = poolX0._2
  val reservesLQ = poolLQ0._2

  val deltaX   = poolX1._2 - reservesX
  val deltaLQ  = poolLQ1._2 - reservesLQ
  val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
  val deltaTMP = poolTMP1._2 - poolTMP0._2

  val curBlockIx    = HEIGHT - programStart + 1
  val curEpochIxRem = curBlockIx % epochLen
  val curEpochIxR   = curBlockIx / epochLen
  val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

  val validAction =
    if (deltaLQ > 0) { // deposit
      val releasedVLQ     = deltaLQ
      val epochsAllocated = epochNum - max(0L, curEpochIx)
      val releasedTMP     = releasedVLQ * epochsAllocated

      deltaLQ == -deltaVLQ &&
      releasedTMP == -deltaTMP
    } else if (deltaLQ < 0) { // redeem
      val releasedLQ        = deltaVLQ
      val epochsDeallocated = epochNum - max(0L, curEpochIx)
      val returnedTMP       = releasedLQ * epochsDeallocated

      deltaVLQ == -deltaLQ &&
      deltaTMP == returnedTMP
    } else { // compound
      val epoch               = successor.R6[Int].get // the epoch we compound
      val epochsToCompound    = epochNum - epoch
      val legalEpoch          = epoch <= curEpochIx - 1
      val prevEpochCompounded = reservesX - epochsToCompound * epochAlloc <= epochAlloc
      val reward              = (epochAlloc.toBigInt * deltaTMP / reservesLQ).toLong

      legalEpoch &&
      prevEpochCompounded &&
      -deltaX == reward &&
      deltaLQ == 0L &&
      deltaVLQ == 0L
    }

  nftPreserved &&
  scriptPreserved &&
  configPreserved &&
  assetsPreserved &&
  noMoreTokens &&
  validCollateral &&
  validAction
}
