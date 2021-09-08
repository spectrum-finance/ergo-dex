package io.ergodex.core.cfmm

import io.ergodex.core.syntax._

class CfmmPoolContractSim(initiallyLockedLP: Long, feeNum: Int, feeDenom: Int, logFn: String => Unit) {

  def run(
    reservesX0: Long,
    reservesY0: Long,
    reservesLP0: Long,
    reservesX1: Long,
    reservesY1: Long,
    reservesLP1: Long,
  ): Boolean = {
    val supplyLP0        = initiallyLockedLP - reservesLP0
    val supplyLP1        = initiallyLockedLP - reservesLP1

    val deltaSupplyLP  = supplyLP1 - supplyLP0
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
        reservesY0.toBigInt * deltaReservesX * feeNum >= -deltaReservesY * (reservesX0.toBigInt * feeDenom + deltaReservesX * feeNum)
      else
        reservesX0.toBigInt * deltaReservesY * feeNum >= -deltaReservesX * (reservesY0.toBigInt * feeDenom + deltaReservesY * feeNum)

    val validAction =
      if (deltaSupplyLP == 0) validSwap
      else if (deltaReservesX > 0 && deltaReservesY > 0) validDepositing
      else validRedemption

    if (deltaSupplyLP == 0) logFn("Swap")
    else if (deltaReservesX > 0 && deltaReservesY > 0) logFn("Deposit")
    else logFn("Redeem")

    validAction
  }
}
