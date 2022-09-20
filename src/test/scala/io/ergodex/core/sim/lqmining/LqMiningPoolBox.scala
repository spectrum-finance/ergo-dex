package io.ergodex.core.sim.lqmining

import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{Box, RuntimeState}
import io.ergodex.core.syntax._

final class LqMiningPoolBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val tokens: Vector[(Coll[Byte], Long)],
  override val registers: Map[Int, Any]
) extends Box[F] {
  override val validatorTag = "lm_pool"

  val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      {
        val poolNFT0 = SELF.tokens(0)
        val poolX0   = SELF.tokens(1)
        val poolLQ0  = SELF.tokens(2)
        val poolVLQ0 = SELF.tokens(3)
        val poolTMP0 = SELF.tokens(4)

        val conf0 = SELF.R4[Coll[Int]].get

        val frameLen     = conf0(0)
        val epochLen     = conf0(1)
        val epochNum     = conf0(2)
        val programStart = conf0(3)

        val programBudget = SELF.R8[Long].get
        val epochAlloc    = programBudget / epochNum

        val successor = OUTPUTS(0)

        val poolNFT1 = successor.tokens(0)
        val poolX1   = successor.tokens(1)
        val poolLQ1  = successor.tokens(2)
        val poolVLQ1 = successor.tokens(3)
        val poolTMP1 = successor.tokens(4)

        val conf1          = successor.R4[Coll[Int]].get
        val programBudget1 = successor.R8[Long].get

        val nftPreserved    = poolNFT1 == poolNFT0
        val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
        val configPreserved = conf1 == conf0 && programBudget1 == programBudget

        val assetsPreserved = poolX1._1 == poolX0._1 &&
          poolLQ1._1 == poolLQ0._1 &&
          poolVLQ1._1 == poolVLQ0._1 &&
          poolTMP1._1 == poolTMP0._1

        // since tokens can be repeated, we ensure for sanity that there are no more tokens
        val noMoreTokens = successor.tokens.size == 5

        val validCollateral = successor.value >= SELF.value

        val lqAllocSum0           = SELF.R5[BigInt].get
        val lastUpdatedAtFrameIx0 = SELF.R6[Int].get
        val lastUpdatedAtEpochIx0 = SELF.R7[Int].get

        val lqAllocSum1           = successor.R5[BigInt].get
        val lastUpdatedAtFrameIx1 = successor.R6[Int].get
        val lastUpdatedAtEpochIx1 = successor.R7[Int].get

        val reservesX  = poolX0._2
        val reservesLQ = poolLQ0._2

        val deltaX   = poolX1._2 - reservesX
        val deltaLQ  = poolLQ1._2 - reservesLQ
        val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
        val deltaTMP = poolTMP1._2 - poolTMP0._2

        val curFrameIxNum = HEIGHT - programStart + 1
        val curFrameIxRem = curFrameIxNum % frameLen
        val curFrameIxR   = curFrameIxNum / frameLen
        val curFrameIx    = if (curFrameIxRem > 0) curFrameIxR + 1 else curFrameIxR
        val curEpochIxRem = curFrameIx    % epochLen
        val curEpochIxR   = curFrameIx / epochLen
        val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

        val validAction =
          if (deltaLQ > 0) { // deposit
            val releasedVLQ     = deltaLQ
            val framesAllocated = epochNum * epochLen - max(0L, curFrameIx)
            val releasedTMP     = releasedVLQ * framesAllocated
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

            deltaLQ == -deltaVLQ &&
            releasedTMP == -deltaTMP &&
            ((lqAllocSum1, lastUpdatedAtFrameIx1), lastUpdatedAtEpochIx1) == nextPoolState
          } else if (deltaLQ < 0) { // redeem
            val releasedLQ        = deltaVLQ
            val framesDeallocated = epochNum * epochLen - max(0L, curFrameIx)
            val returnedTMP       = releasedLQ * framesDeallocated
            val lqAllocSum_ =
              if (lastUpdatedAtFrameIx0 == curFrameIx) lqAllocSum0 - releasedLQ
              else lqAllocSum0

            deltaVLQ == -deltaLQ &&
            deltaTMP == returnedTMP &&
            lqAllocSum1 == lqAllocSum_ &&
            lastUpdatedAtFrameIx1 == lastUpdatedAtFrameIx0 &&
            lastUpdatedAtEpochIx1 == lastUpdatedAtEpochIx0
          } else { // compound
            val epoch               = successor.R9[Int].get // the epoch we compound
            val epochsToCompound    = epochNum - epoch
            val legalEpoch          = epoch <= curEpochIx - 1
            val prevEpochCompounded = reservesX - epochsToCompound * epochAlloc <= epochAlloc
            val lqAllocSum_ =
              if (lastUpdatedAtEpochIx0 != (curEpochIx - 1)) {
                reservesLQ.toBigInt * epochLen // reserves haven't been updated for the whole past epoch.
              } else if (lastUpdatedAtFrameIx0 != epoch * epochLen) {
                val framesUntouched = epoch * epochLen - lastUpdatedAtFrameIx0
                reservesLQ.toBigInt * framesUntouched + lqAllocSum0
              } else {
                lqAllocSum0
              }
            val reward = epochAlloc.toBigInt * deltaTMP / lqAllocSum_
            val state0 = ((lqAllocSum0, lastUpdatedAtFrameIx0), lastUpdatedAtEpochIx0)
            val state1 = ((lqAllocSum1, lastUpdatedAtFrameIx1), lastUpdatedAtEpochIx1)

            legalEpoch &&
            prevEpochCompounded &&
            -deltaX == reward &&
            deltaLQ == 0L &&
            deltaVLQ == 0L &&
            state1 == state0
          }

        nftPreserved &&
        scriptPreserved &&
        configPreserved &&
        assetsPreserved &&
        noMoreTokens &&
        validCollateral &&
        validAction
      }
    }
}
