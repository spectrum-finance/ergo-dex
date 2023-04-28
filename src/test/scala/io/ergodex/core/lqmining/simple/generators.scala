package io.ergodex.core.lqmining.simple

import org.scalacheck.Gen

object generators {

  def lmConfGen: Gen[LMConfig] =
    for {
      epochLen         <- Gen.chooseNum(10, 200000)
      epochNum         <- Gen.const(3)
      programStart     <- Gen.chooseNum(920905, 1920905)
      redeemLimitDelta <- Gen.chooseNum(1, 10000)
      maxRoundingError <- Gen.const(epochNum)
      programBudget    <- Gen.chooseNum(100L, Long.MaxValue)
    } yield LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, programBudget, maxRoundingError)

  def DepositGen: Gen[Long] =
    for {
      lq <- Gen.chooseNum[Long](minT = 1L, maxT = Int.MaxValue)
    } yield lq

  def MultGen: Gen[(Long, Long, Long)] =
    for {
      m1 <- Gen.chooseNum[Long](minT = 1L, maxT = 477218588L)
      m2 <- Gen.chooseNum[Long](minT = 1L, maxT = 477218588L)
      m3 <- Gen.chooseNum[Long](minT = 1L, maxT = 477218588L)
    } yield (m1, m2, m3)
}
