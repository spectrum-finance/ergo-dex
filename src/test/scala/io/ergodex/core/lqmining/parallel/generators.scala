package io.ergodex.core.lqmining.parallel

import org.scalacheck.Gen

object generators {

  def lmConfGen: Gen[LMConfig] =
    for {
      epochLen         <- Gen.chooseNum(10, 200000)
      epochNum         <- Gen.const(3)
      programStart     <- Gen.chooseNum(920905, 1920905)
      redeemLimitDelta <- Gen.chooseNum(1, 10000)
      maxRoundingError <- Gen.const(epochNum)
      mainBudget <- Gen.chooseNum(10000L, Long.MaxValue / 4)
      optBudget  <- Gen.chooseNum(1L, Long.MaxValue / 4)

    } yield LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, maxRoundingError, mainBudget, optBudget)

  def DepositGen: Gen[Long] =
    for {
      lq <- Gen.chooseNum[Long](minT = 1000L, maxT = Int.MaxValue)
    } yield lq

  def MultGen: Gen[(Long, Long, Long)] =
    for {
      m1 <- Gen.chooseNum[Long](minT = 1L, maxT = 3L)
      m2 <- Gen.chooseNum[Long](minT = 1L, maxT = 3L)
      m3 <- Gen.chooseNum[Long](minT = 1L, maxT = 3L)
    } yield (m1, m2, m3)
}
