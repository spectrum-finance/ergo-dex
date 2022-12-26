package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.{Box, RuntimeState}
import io.ergodex.core.syntax._

final class CfmmPoolBox[F[_] : RuntimeState](
                                              override val id: Coll[Byte],
                                              override val value: Long,
                                              override val creationHeight: Int,
                                              override val tokens: Vector[(Coll[Byte], Long)],
                                              override val registers: Map[Int, Any]
                                            ) extends Box[F] {
  override val validatorTag = "cfmm_pool_box"

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      val InitiallyLockedLP = 0x7fffffffffffffffL
      val FeeDenom = 1000
      val MinStorageRent = 1L // this many number of nanoErgs are going to be permanently locked

      val poolNFT0 = SELF.tokens(0)
      val reservedLP0 = SELF.tokens(1)
      val tokenY0 = SELF.tokens(2)

      val successor = OUTPUTS(0)

      val feeNum0 = SELF.R4[Int].get
      val feeNum1 = successor.R4[Int].get

      val poolNFT1 = successor.tokens(0)
      val reservedLP1 = successor.tokens(1)
      val tokenY1 = successor.tokens(2)

      val validSuccessorScript = successor.propositionBytes == SELF.propositionBytes
      val preservedFeeConfig = feeNum1 == feeNum0

      val preservedPoolNFT = poolNFT1 == poolNFT0
      val validLP = reservedLP1._1 == reservedLP0._1
      val validY = tokenY1._1 == tokenY0._1
      // since tokens can be repeated, we ensure for sanity that there are no more tokens
      val noMoreTokens = successor.tokens.size == 3

      val validStorageRent = successor.value > MinStorageRent

      val supplyLP0 = InitiallyLockedLP - reservedLP0._2
      val supplyLP1 = InitiallyLockedLP - reservedLP1._2

      val reservesX0 = SELF.value
      val reservesY0 = tokenY0._2
      val reservesX1 = successor.value
      val reservesY1 = tokenY1._2

      val deltaSupplyLP = supplyLP1 - supplyLP0
      val deltaReservesX = reservesX1 - reservesX0
      val deltaReservesY = reservesY1 - reservesY0

      val validDepositing = {
        val sharesUnlocked = min(
          deltaReservesX.toBigInt * supplyLP0 / reservesX0,
          deltaReservesY.toBigInt * supplyLP0 / reservesY0
        )
        deltaSupplyLP <= sharesUnlocked
      }

      val validRedemption = {
        val _deltaSupplyLP = deltaSupplyLP.toBigInt
        // note: _deltaSupplyLP, deltaReservesX and deltaReservesY are negative
        deltaReservesX.toBigInt * supplyLP0 >= _deltaSupplyLP * reservesX0 && deltaReservesY.toBigInt * supplyLP0 >= _deltaSupplyLP * reservesY0
      }

      val validSwap =
        if (deltaReservesX > 0)
          reservesY0.toBigInt * deltaReservesX * feeNum0 >= -deltaReservesY * (reservesX0.toBigInt * FeeDenom + deltaReservesX * feeNum0)
        else
          reservesX0.toBigInt * deltaReservesY * feeNum0 >= -deltaReservesX * (reservesY0.toBigInt * FeeDenom + deltaReservesY * feeNum0)

      val validAction =
        if (deltaSupplyLP == 0)
          validSwap
        else if (deltaReservesX > 0 && deltaReservesY > 0) validDepositing
        else validRedemption

      validSuccessorScript &&
        preservedFeeConfig &&
        preservedPoolNFT &&
        validLP &&
        validY &&
        noMoreTokens &&
        validAction &&
        validStorageRent
    }
}
