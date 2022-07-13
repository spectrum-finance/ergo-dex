package io.ergodex.core.sim.lqmining

import io.ergodex.core.sim.{LedgerCtx, LedgerPlatform}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class LQMiningPoolSpec extends AnyFlatSpec with should.Matchers with LedgerPlatform {

  val KK = 1000000L

  val pool0 = LMPool.init[Ledger](frameLen = 1, epochLen = 1, epochNum = 3, programStart = 1, programBudget = 90)

  val input0 = Asset[Token.LQ](1 * KK)

  it should "return correct amount of bundled tokens on deposit" in {
    val action                   = pool0.deposit(input0)
    val (_, Right((_, bundle1))) = action.run(LedgerCtx.init).value
    bundle1 shouldBe StakingBundle(input0.value, pool0.conf.epochNum)
  }

  it should "return correct pool state on deposit" in {
    val action                       = pool0.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(LedgerCtx.init).value
    pool1.reserves.LQ shouldBe input0.value
    pool1.reserves.vLQ shouldBe LMPool.MaxCapVLQ - bundle1.vLQ
    pool1.reserves.TT shouldBe LMPool.MaxCapTT - bundle1.TT
  }

  it should "do nothing on premature compounding attempt" in {
    val action = for {
      Right((pool1, bundle1)) <- pool0.deposit(input0)
      res                     <- pool1.compound(bundle1)
    } yield (pool1, bundle1, res)
    val (_, (pool1, bundle1, Right((pool2, bundle2, output)))) = action.run(LedgerCtx.init).value
    output shouldBe Asset(0L)
    pool2 shouldBe pool1
    bundle2 shouldBe bundle1
  }

  it should "release correct amount of reward on compounding" in {
    val action = for {
      Right((pool1, bundle1)) <- pool0.deposit(input0)
      _                       <- Ledger.extend
      res                     <- pool1.compound(bundle1)
    } yield (pool1, bundle1, res)
    val (s, (pool1, bundle1, Right((pool2, bundle2, output)))) = action.run(LedgerCtx.init).value
    output shouldBe Asset(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TT = bundle1.TT - pool2.conf.epochLen)
  }

  it should "not allow compounding while previous epoch is not fully compounded" in {
    val action = for {
      Right((pool1, bundle1)) <- pool0.deposit(input0)
      _                       <- Ledger.extendBy(2)
      res                     <- pool1.compound(bundle1)
    } yield res
    val (_, res) = action.run(LedgerCtx.init).value
    res shouldBe Left(LMPool.PrevEpochNotWithdrawn)
  }

  it should "not allow to compound already fully compounded epoch" in {
    val action = for {
      Right((pool1, bundle1))    <- pool0.deposit(input0)
      _                          <- Ledger.extend
      Right((pool2, bundle2, _)) <- pool1.compound(bundle1)
      res                        <- pool2.compound(bundle2)
    } yield res
    val (_, res) = action.run(LedgerCtx.init).value
    res shouldBe Left(LMPool.EpochAlreadyWithdrawn)
  }

  it should "output zero reward on an attempt to compound on same epoch more than once" in {}

  it should "allow late compounding" in {}

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
