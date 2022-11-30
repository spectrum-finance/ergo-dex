package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.RuntimeState.withRuntimeState
import io.ergodex.core.sim.{Box, RuntimeState}
import io.ergodex.core.syntax._

final class LqMiningPoolBoxSelfHosted[F[_] : RuntimeState](
                                                            override val id: Coll[Byte],
                                                            override val value: Long,
                                                            override val tokens: Vector[(Coll[Byte], Long)],
                                                            override val registers: Map[Int, Any]
                                                          ) extends Box[F] {
  override val validatorTag = "lm_pool_self"

  val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // ===== Contract Information ===== //
      // Name: LMPoolSelfHosted
      // Description: Contract that validates a change in the LM pool's state.

      // ===== LM Pool Box ===== //
      // Registers:
      //   R4[Coll[Int]]: LM program config
      //      0: Length of every epoch in blocks
      //      1: Number of epochs in the LM program
      //      2: Program start
      //   R5[Long]: Program budget  // total budget of LM program.
      //   R7[Int]: Epoch index  // index of the epoch being compounded (required only for compounding).
      //   R8[Long]: MinValue // ERG min Value.
      //
      // Tokens:
      //   0:
      //     _1: LM Pool NFT
      //     _2: Amount: 1
      //   1:
      //     _1: Reward Token ID
      //     _2: Amount: <= Program budget.
      //   2:
      //     _1: LQ Token ID  // locked LQ tokens.
      //     _2: Amount of LQ tokens.
      //   3:
      //     _1: vLQ Token ID  // tokens representing locked share of LQ.
      //     _2: Amount of vLQ tokens.
      //   4:
      //     _1: TMP Token ID  // left program epochs times liquidity.
      //     _2: Amount of TMP tokens.
      //
      // Validations:
      // 1. LM Pool NFT is preserved;
      // 2. LM Pool Config, LM program budget and minValue are preserved;
      // 3. LMPool validation script is preserved;
      // 4. LM Pool assets are preserved;
      // 5. There are no illegal tokens in LM Pool;
      // 6. Action is valid:
      //    6.1. Deposit: if (deltaLQ > 0)
      //         6.1.1. Previous epochs are compounded;
      //         6.1.2. Delta LQ tokens amount is correct;
      //         6.1.3. Delta TMP tokens amount is correct.
      //    6.2. Redeem: elif if (deltaLQ < 0)
      //         6.2.1. Previous epochs are compounded;
      //         6.2.2. Delta LQ tokens amount is correct;
      //         6.2.3. Delta TMP tokens amount is correct.
      //    6.3. Compound: else
      //         6.3.1. Epoch is legal to perform compounding;
      //         6.3.2. Previous epoch is compounded;
      //         6.3.3. Delta reward tokens amount equals to calculated reward amount;
      //         6.3.4. Delta LQ tokens amount is 0;
      //         6.3.5. Delta vLQ tokens amount is 0.
      //
      // ===== Getting SELF data ===== //
      val poolNFT0 = SELF.tokens(0)
      val poolX0 = SELF.tokens(1)
      val poolLQ0 = SELF.tokens(2)
      val poolVLQ0 = SELF.tokens(3)
      val poolTMP0 = SELF.tokens(4)

      val conf0 = SELF.R4[Coll[Int]].get
      val epochLen = conf0(0)
      val epochNum = conf0(1)
      val programStart = conf0(2)

      val programBudget0 = SELF.R5[Long].get
      val minValue0 = SELF.R8[Long].get

      // ===== Getting OUTPUTS data ===== //
      val successor = OUTPUTS(0)

      val poolNFT1 = successor.tokens(0)
      val poolX1 = successor.tokens(1)
      val poolLQ1 = successor.tokens(2)
      val poolVLQ1 = successor.tokens(3)
      val poolTMP1 = successor.tokens(4)

      val conf1 = successor.R4[Coll[Int]].get

      val programBudget1 = successor.R5[Long].get
      val minValue1 = successor.R8[Long].get

      // ===== Getting deltas ===== //
      val reservesX = poolX0._2
      val reservesLQ = poolLQ0._2

      val deltaX = poolX1._2 - reservesX
      val deltaLQ = poolLQ1._2 - reservesLQ
      val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
      val deltaTMP = poolTMP1._2 - poolTMP0._2

      // ===== Calculating epoch parameters ===== //
      val epochAlloc = programBudget0 / epochNum
      val curBlockIx = HEIGHT - programStart + 1
      val curEpochIxRem = curBlockIx % epochLen
      val curEpochIxR = curBlockIx / epochLen
      val curEpochIx = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

      // ===== Validating conditions ===== //
      // 1.
      val nftPreserved = poolNFT1 == poolNFT0
      // 2.
      val configPreserved =
        (conf1 == conf0) &&
          (programBudget1 == programBudget0) &&
          (minValue1 == minValue0)

      // 3.
      val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
      // 4.
      val assetsPreserved =
        poolX1._1 == poolX0._1 &&
          poolLQ1._1 == poolLQ0._1 &&
          poolVLQ1._1 == poolVLQ0._1 &&
          poolTMP1._1 == poolTMP0._1
      // 5.
      val noMoreTokens = successor.tokens.size == 5
      // 6.
      val validAction = {
        if (deltaLQ > 0) { // deposit
          // 6.1.
          val releasedVLQ = deltaLQ
          val epochsAllocated = epochNum - max(0L, curEpochIx)
          val releasedTMP = releasedVLQ * epochsAllocated
          // 6.1.1.
          val prevEpochsCompoundedForDeposit = reservesX - (epochNum - curEpochIx + 1) * programBudget0 / epochNum + minValue0 >= epochAlloc

          (prevEpochsCompoundedForDeposit || (reservesX == programBudget0)) &&
            // 6.1.2. && 6.1.3.
            (deltaLQ == -deltaVLQ) &&
            (releasedTMP == -deltaTMP)

        } else if (deltaLQ < 0) { // redeem
          // 6.2.
          val releasedLQ = deltaVLQ
          val curEpochToCalc = if (curEpochIx < epochNum) curEpochIx else epochNum + 1
          val minReturnedTMP = {
            if (curEpochIx > epochNum) 0L
            else {
              val epochsDeallocated = epochNum - max(0L, curEpochIx)
              releasedLQ * epochsDeallocated
            }
          }
          // 6.2.1.
          val prevEpochsCompoundedForRedeem = ((programBudget0 - reservesX) + minValue0) >= (curEpochToCalc - 1) * programBudget0 / epochNum

          (prevEpochsCompoundedForRedeem || (reservesX == programBudget0)) &&
            // 6.2.2. & 6.2.3.
            (deltaVLQ == -deltaLQ) &&
            (deltaTMP >= minReturnedTMP)

        }
        else { // compound
          // 6.3.
          val epoch = successor.R7[Int].get
          val epochsToCompound = epochNum - epoch
          val prevEpochCompounded = (reservesX - epochsToCompound * programBudget0.toBigInt / epochNum).toLong <= (epochAlloc + minValue0)

          val legalEpoch = epoch <= curEpochIx - 1
          val reward = (epochAlloc.toBigInt * deltaTMP / reservesLQ).toLong

          // 6.3.1. && 6.3.2. && 6.3.3. && 6.3.4. && 6.3.5.
          legalEpoch &&
            prevEpochCompounded &&
            (-deltaX == reward) &&
            (deltaLQ == 0L) &&
            (deltaVLQ == 0L)
        }
      }

      nftPreserved &&
        configPreserved &&
        scriptPreserved &&
        assetsPreserved &&
        noMoreTokens &&
        validAction
    }
}