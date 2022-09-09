{
  val poolNFT0 = SELF.tokens(0)
  val poolX0   = SELF.tokens(1)
  val poolLQ0  = SELF.tokens(2)
  val poolVLQ0 = SELF.tokens(3)
  val poolTT0  = SELF.tokens(4)

  val conf0 = SELF.R4[Coll[Int]].get

  val frameLen      = conf0(0)
  val epochLen      = conf0(1)
  val epochNum      = conf0(2)
  val programStart  = conf0(3)
  val programBudget = conf0(4)

  val epochAlloc = programBudget / epochNum

  val successor = OUTPUTS(0)

  val poolNFT1 = successor.tokens(0)
  val poolX1   = successor.tokens(1)
  val poolLQ1  = successor.tokens(2)
  val poolVLQ1 = successor.tokens(3)
  val poolTT1  = successor.tokens(4)

  val conf1 = successor.R4[Coll[Int]].get

  val nftPreserved    = poolNFT1 == poolNFT0
  val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
  val configPreserved = conf1 == conf0

  val assetsPreserved = poolX1._1 == poolX0._1 &&
                        poolLQ1._1 == poolLQ0._1 &&
                        poolVLQ1._1 == poolVLQ0._1 &&
                        poolTT1._1 == poolTT0._1

  // since tokens can be repeated, we ensure for sanity that there are no more tokens
  val noMoreTokens = successor.tokens.size == 5

  val validCollateral = successor.value >= MinCollateral

  val lqAllocSum0           = SELF.R5[BigInt].get
  val lastUpdatedAtFrameIx0 = SELF.R6[Int].get
  val lastUpdatedAtEpochIx0 = SELF.R7[Int].get

  val lqAllocSum1           = successor.R5[BigInt].get
  val lastUpdatedAtFrameIx1 = successor.R6[Int].get
  val lastUpdatedAtEpochIx1 = successor.R7[Int].get

  val reservesX = poolX0._2
  val reservesLQ = poolLQ0._2

  val deltaX   = poolX1._2 - reservesX
  val deltaLQ  = poolLQ1._2 - reservesLQ
  val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
  val deltaTT  = poolTT1._2 - poolTT0._2

  val curFrameIxNum = HEIGHT - programStart + 1
  val curFrameIxRem = curFrameIxNum % frameLen
  val curFrameIxR   = curFrameIxNum / frameLen
  val curFrameIx    = if (curFrameIxRem > 0) curFrameIxR + 1 else curFrameIxR
  val curEpochIxRem = curFrameIx % epochLen
  val curEpochIxR   = curFrameIx / epochLen
  val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

  val epochsToCompound = epochNum - curEpochIx

  val height = HEIGHT

  val validAction =
    if (deltaLQ > 0) {
      val prevEpochCompounded = reservesX - epochsToCompound * epochAlloc <= epochAlloc
      
      val releasedVLQ   = deltaLQ
      val releasedTT    = epochNum * epochLen - max(0, curFrameIx)
      val nextPoolState =
        if (curEpochIx != lastUpdatedAtEpochIx0) {
          val passedFrames = curFrameIx - (curEpochIx - 1) * epochLen
          ((passedFrames.toBigInt * reservesLQ, curFrameIx), curFrameIx)
        } else if (curFrameIx != lastUpdatedAtFrameIx0) {
          val passedFrames = curFrameIx - lastUpdatedAtFrameIx0
          ((lqAllocSum0 + passedFrames * reservesLQ, curFrameIx), lastUpdatedAtEpochIx0)
        } else {
          ((lqAllocSum0, lastUpdatedAtFrameIx0), lastUpdatedAtEpochIx0)
        }

      prevEpochCompounded &&
      deltaLQ == -deltaVLQ &&
      releasedTT == -deltaTT &&
      ((lqAllocSum1, lastUpdatedAtFrameIx1), lastUpdatedAtEpochIx1) == nextPoolState
    } else if (deltaLQ < 0) {
      val releasedLQ  = deltaVLQ
      val returnedTT  = epochNum * epochLen - max(0, curFrameIx)
      val lqAllocSum_ =
        if (lastUpdatedAtFrameIx0 == curFrameIx) lqAllocSum0 - releasedLQ
        else lqAllocSum0

      deltaVLQ == -deltaLQ &&
      deltaTT == returnedTT &&
      lqAllocSum1 == lqAllocSum_ &&
      lastUpdatedAtFrameIx1 == lastUpdatedAtFrameIx0 &&
      lastUpdatedAtEpochIx1 == lastUpdatedAtEpochIx0
    } else {
      val prevEpochCompounded = reservesX - epochsToCompound * epochAlloc <= epochAlloc
      val epochsToCompound    = epochNum - epoch
      val inputEpochTT        = bundle.TT - epochLen * epochsToCompound
      val lqAllocSum_ =
        if (lastUpdatedAtEpochIx0 != (curEpochIx - 1)) {
          reservesLQ * epochLen // reserves haven't been updated for the whole past epoch.
        } else if (lastUpdatedAtFrameIx0 != epoch * epochLen) {
          val framesUntouched = epoch * epochLen - lastUpdatedAtFrameIx0
          reservesLQ * framesUntouched + lqAllocSum0
        } else {
          lqAllocSum0
        }
      val reward =
        BigInt(epochAlloc) * inputEpochTT * bundle.vLQ * epochLen /
          (BigInt(lqAllocSum_) * epochLen)
      
      prevEpochCompounded &&

    }

  nftPreserved &&
  scriptPreserved &&
  configPreserved &&
  assetsPreserved &&
  noMoreTokens &&
  validCollateral &&
  validAction
}