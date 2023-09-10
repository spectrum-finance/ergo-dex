package io.ergodex.core.lqmining.extended

import org.scalacheck.Gen

object generators {

  def lmConfGen: Gen[LMConfig] =
    for {
      epochLen         <- Gen.chooseNum(10, 200000)
      epochNum         <- Gen.const(3)
      programStart     <- Gen.chooseNum(920905, 1920905)
      redeemLimitDelta <- Gen.chooseNum(1, 10000)
      maxRoundingError <- Gen.const(epochNum)

    } yield LMConfig(epochLen, epochNum, programStart, redeemLimitDelta, maxRoundingError)

  def budgetsGen: Gen[ActualBudgets] =
    for {
      mainBudget <- Gen.chooseNum(1000L, Long.MaxValue)
      optBudget  <- Gen.chooseNum(0L, Long.MaxValue)

    } yield ActualBudgets(mainBudget, optBudget)

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
