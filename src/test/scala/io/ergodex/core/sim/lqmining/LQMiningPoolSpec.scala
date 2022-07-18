package io.ergodex.core.sim.lqmining

import io.ergodex.core.sim.lqmining.Token.LQ
import io.ergodex.core.sim.{LedgerCtx, LedgerPlatform}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tofu.syntax.monadic._

class LQMiningPoolSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  val KK = 1000000L

  val pool01: LMPool[Ledger] =
    LMPool.init(frameLen = 1, epochLen = 1, epochNum = 3, programStart = 2, programBudget = 90)
  val pool02: LMPool[Ledger] =
    LMPool.init(frameLen = 1, epochLen = 2, epochNum = 3, programStart = 2, programBudget = 900000L)

  val input0: AssetInput[LQ] = AssetInput(1 * KK)
  val input1: AssetInput[LQ] = AssetInput(2 * KK)

  it should "return correct amount of bundled tokens on deposit" in {
    forAll(Gen.choose(0, pool01.conf.epochNum)) { span =>
      val action                   = Ledger.extendBy(span) >> pool01.deposit(input0)
      val (_, Right((_, bundle1))) = action.run(LedgerCtx.init).value
      bundle1 shouldBe StakingBundle(input0.value, pool01.conf.epochNum - span)
    }
  }

  it should "return correct amount of bundled tokens on deposit (before start)" in {
    val pool = LMPool.init(frameLen = 1, epochLen = 1, epochNum = 3, programStart = 5, programBudget = 90)

    val action                   = pool.deposit(input0)
    val (_, Right((_, bundle1))) = action.run(LedgerCtx.init).value
    bundle1 shouldBe StakingBundle(input0.value, pool.conf.epochNum)
  }

  it should "return correct amount of bundled tokens on deposit (fractional epoch)" in {
    val maxHeight = pool02.conf.epochNum * pool02.conf.epochLen
    val maxFrames = maxHeight
    forAll(Gen.choose(0, maxHeight)) { span =>
      val action                   = Ledger.extendBy(span) >> pool02.deposit(input0)
      val (_, Right((_, bundle1))) = action.run(LedgerCtx.init).value
      bundle1 shouldBe StakingBundle(input0.value, maxFrames - span * pool02.conf.frameLen)
    }
  }

  it should "return correct pool state on deposit" in {
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(LedgerCtx.init).value
    pool1.reserves.LQ shouldBe input0.value
    pool1.reserves.vLQ shouldBe LMPool.MaxCapVLQ - bundle1.vLQ
    pool1.reserves.TT shouldBe LMPool.MaxCapTT - bundle1.TT
  }

  it should "fail on premature compounding attempt" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield res
    val (_, res) = action.run(LedgerCtx.init).value
    res shouldBe Left(LMPool.IllegalEpoch)
  }

  it should "release correct amount of reward on compounding" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      _                       <- Ledger.extendBy(2)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield (pool1, bundle1, res)
    val (_, (pool1, bundle1, Right((pool2, bundle2, output, _)))) = action.run(LedgerCtx.init).value
    output shouldBe AssetOutput(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TT = bundle1.TT - pool2.conf.epochLen)
  }

  it should "release correct amount of reward on compounding (fractional epoch)" in {
    val action = for {
      Right((pool1, bundle11))             <- pool02.deposit(input1)
      _                                    <- Ledger.extend
      _ = println((pool1, bundle11))
      Right((pool2, bundle21))             <- pool1.deposit(input0)
      _                                    <- Ledger.extendBy(4)
      _ = println((pool2, bundle21))
      Right((pool3, bundle12, output1, _)) <- pool2.compound(bundle11, epoch = 1)
      _ = println((pool3, bundle12, output1))
      Right((pool4, bundle22, output2, _)) <- pool3.compound(bundle21, epoch = 1)
      _ = println((pool4, bundle22, output2))
      _                                    <- Ledger.extendBy(2)
      Right((pool5, bundle13, output3, _)) <- pool4.compound(bundle12, epoch = 2)
      _ = println((pool5, bundle13, output3))
      _                                    <- Ledger.extendBy(2)
      Right((pool6, bundle14, output5, _)) <- pool5.compound(bundle13, epoch = 3)
      _ = println((pool6, bundle14, output5))
      Right((pool7, bundle23, output4, _)) <- pool6.compound(bundle22, epoch = 2)
      _ = println((pool7, bundle23, output4))
      Right((pool8, bundle24, output6, _)) <- pool7.compound(bundle23, epoch = 3)
      _ = println((pool8, bundle24, output6))
    } yield (output1, output2, output3, output4, output5, output6)
    val (_, (output1, output2, output3, output4, output5, output6)) = action.run(LedgerCtx.init).value
    println((output1, output2, output3, output4, output5, output6))
  }

  it should "do nothing on an attempt to compound already fully compounded epoch" in {
    val action = for {
      Right((pool1, bundle1))                 <- pool01.deposit(input0)
      _                                       <- Ledger.extendBy(2)
      Right((pool2, bundle2, _, _))           <- pool1.compound(bundle1, epoch = 1)
      Right((pool3, bundle3, reward, burned)) <- pool2.compound(bundle2, epoch = 1)
    } yield (pool2, pool3, bundle2, bundle3, reward, burned)
    val (_, (pool2, pool3, bundle2, bundle3, reward, burned)) = action.run(LedgerCtx.init).value
    pool3 shouldBe pool2
    bundle2 shouldBe bundle3
    reward shouldBe AssetOutput(0L)
    burned shouldBe BurnAsset(0L)
  }

  it should "do nothing on an attempt to compound on same epoch more than once" in {
    val action = for {
      Right((pool1, bundle11))            <- pool01.deposit(input0)
      Right((pool2, _))                   <- pool1.deposit(input1)
      _                                   <- Ledger.extendBy(2)
      Right((pool3, bundle21, _, _))      <- pool2.compound(bundle11, epoch = 1)
      Right((pool4, bundle31, reward, _)) <- pool3.compound(bundle21, epoch = 1)
    } yield (pool3, pool4, bundle21, bundle31, reward)
    val (_, (pool3, pool4, bundle21, bundle31, reward)) = action.run(LedgerCtx.init).value
    pool4 shouldBe pool3
    bundle31 shouldBe bundle21
    reward shouldBe AssetOutput(0L)
  }

  it should "allow late compounding" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      _                       <- Ledger.extendBy(4)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield (pool1, bundle1, res)
    val (_, (pool1, bundle1, Right((pool2, bundle2, output, _)))) = action.run(LedgerCtx.init).value
    output shouldBe AssetOutput(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TT = bundle1.TT - pool2.conf.epochLen)
  }

  //  val scenario0 =
//    for {
//      Right((pool1, bundle1)) <- pool0.deposit(input0)
//      _ = bundle1 shouldBe StakingBundle(input0.value, pool0.conf.epochNum)
//      _ = pool1.reserves.LQ shouldBe input0.value
//      _ = pool1.reserves.vLQ shouldBe LMPool.MaxCapVLQ - bundle1.vLQ
//      _ = pool1.reserves.TT shouldBe LMPool.MaxCapTT - bundle1.TT
//      _ <- Ledger.extend
//      Right((pool2, bundle2, out2)) <- pool1.compound(bundle1)
//      _ = println((pool2, bundle2, out2))
//    } yield ()
//
//  scenario0.run(LedgerCtx.init).value
}
