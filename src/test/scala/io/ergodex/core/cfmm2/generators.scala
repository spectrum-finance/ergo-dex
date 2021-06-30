package io.ergodex.core.cfmm2

import org.scalacheck.Gen

object generators {

  def feeGen: Gen[Fee] =
    for {
      denom  <- Gen.oneOf(Seq(10, 100, 1000, 10000))
      feeNum <- Gen.chooseNum(1, denom - 1)
    } yield Fee(feeNum, denom)

  def poolGen: Gen[CfmmPool] =
    for {
      fee   <- feeGen
      minXY <- Gen.oneOf(Seq(1000, 2000, 10000, 20000, 100000, 1000000))
      x     <- Gen.chooseNum[Long](minXY, maxT = Int.MaxValue)
      y     <- Gen.chooseNum[Long](minXY, maxT = Int.MaxValue)
    } yield new CfmmPool(x, y, fee)

  def depositSellGen: Gen[Seq[CfmmOp]] =
    for {
      numOps <- Gen.chooseNum(minT = 1, maxT = 2000)
      ops    <- Gen.listOfN(numOps, Gen.pick(1, depositGen, sellXGen, sellYGen).map(_.head))
    } yield ops

  def depositGen: Gen[Deposit] =
    for {
      x <- Gen.chooseNum[Long](minT = 1, maxT = Short.MaxValue)
      y <- Gen.chooseNum[Long](minT = 1, maxT = Short.MaxValue)
    } yield Deposit(x, y)

  def sellXGen: Gen[SellX] =
    for {
      amount <- Gen.chooseNum[Long](minT = 1L, maxT = Short.MaxValue)
    } yield SellX(amount)

  def sellYGen: Gen[SellY] =
    for {
      amount <- Gen.chooseNum[Long](minT = 1L, maxT = Short.MaxValue)
    } yield SellY(amount)

}
