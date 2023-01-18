package io.ergodex.core.lqmining.simple

import org.scalacheck.Gen

object generators {

  def lmConfGen: Gen[LMConfig] =
    for {
      epochLen         <- Gen.chooseNum(3, 200000)
      epochNum         <- Gen.const(3)
      programStart     <- Gen.chooseNum(920905, 1920905)
      redeemLimitDelta <- Gen.chooseNum(1, 10000)
      programBudget    <- Gen.chooseNum(60L, Long.MaxValue)
      maxRoundingError <- Gen.const(9L)
    } yield LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, programBudget, maxRoundingError)

  def DepositGen(minDeposit: Long): Gen[Long] =
    for {
      lq <- Gen.chooseNum[Long](minDeposit, maxT = Int.MaxValue)
    } yield lq
}
