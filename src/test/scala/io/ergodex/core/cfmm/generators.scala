package io.ergodex.core.cfmm

import org.scalacheck.Gen

object generators {

  def cfmmConfGen: Gen[PoolConfig] =
    for {
      feeNum     <- Gen.chooseNum(501, 999)
      feeDenom   <- Gen.const(1000)
      emissionLP <- Gen.const(Long.MaxValue)
      burnLP     <- Gen.const(1000L)
      minDeposit <- Gen.oneOf(Seq(1000, 2000, 10000, 20000))
    } yield PoolConfig(feeNum, feeDenom, emissionLP, burnLP, minDeposit)

  def initialDepositGen(minDeposit: Long): Gen[(Long, Long)] =
    for {
      x <- Gen.chooseNum[Long](minDeposit, maxT = Int.MaxValue)
      y <- Gen.chooseNum[Long](minDeposit, maxT = Int.MaxValue)
    } yield (x, y)

  def depositSwapGen: Gen[Seq[CfmmOp]] =
    for {
      numOps <- Gen.chooseNum(minT = 1, maxT = 2000)
      ops    <- Gen.listOfN(numOps, Gen.pick(1, depositGen, swapGen).map(_.head))
    } yield ops

  def depositGen: Gen[Deposit] =
    for {
      x <- Gen.chooseNum[Long](minT = 1, maxT = Short.MaxValue)
      y <- Gen.chooseNum[Long](minT = 1, maxT = Short.MaxValue)
    } yield Deposit(x, y)

  def swapGen: Gen[Swap] =
    for {
      asset  <- Gen.oneOf(Seq("x", "y"))
      amount <- Gen.chooseNum[Long](minT = 1L, maxT = Short.MaxValue)
    } yield Swap(asset, amount)
}
