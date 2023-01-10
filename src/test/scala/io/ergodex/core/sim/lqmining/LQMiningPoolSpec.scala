package io.ergodex.core.sim.lqmining

import io.ergodex.core.sim.lqmining.LMPool.PrevEpochNotWithdrawn
import io.ergodex.core.sim.lqmining.Token.LQ
import io.ergodex.core.sim.{LedgerPlatform, RuntimeCtx}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tofu.syntax.monadic._
import cats.syntax.semigroup._

class LQMiningPoolSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  val KK = 1000000L

  val pool01: LMPool[Ledger] =
    LMPool.init(frameLen = 1, epochLen = 1, epochNum = 3, programStart = 2, programBudget = 90)
  val pool02: LMPool[Ledger] =
    LMPool.init(frameLen = 1, epochLen = 2, epochNum = 3, programStart = 2, programBudget = 900000000L)

  val input0: AssetInput[LQ] = AssetInput(1 * KK)
  val input1: AssetInput[LQ] = AssetInput(2 * KK)

  it should "return correct amount of bundled tokens on deposit" in {
    forAll(Gen.choose(0, pool01.conf.frameLen * pool01.conf.epochLen)) { span =>
      whenever(span >= 0) {
        val action                   = Ledger.extendBy(span) >> pool01.deposit(input0)
        val (_, Right((_, bundle1))) = action.run(RuntimeCtx.init).value
        bundle1 shouldBe StakingBundle(input0.value, input0.value * (pool01.conf.epochNum - span))
      }
    }
  }

  it should "aggregate vLQ allocations (deposits in non-adjacent frames)" in {
    val pool       = LMPool.init(frameLen = 1, epochLen = 10, epochNum = 2, programStart = 3, programBudget = 100)
    val skipFrames = 5
    val action = for {
      Right((pool1, _)) <- pool.deposit(input0)
      ctx               <- Ledger.ctx
      extendBy = (pool.conf.programStart - 1 - ctx.height) + skipFrames * pool.conf.frameLen
      _                 <- Ledger.extendBy(extendBy)
      Right((pool2, _)) <- pool1.deposit(input0)
    } yield pool2
    val (_, pl) = action.run(RuntimeCtx.init).value
    pl.lqAllocSum shouldBe input0.value * skipFrames
  }

  it should "aggregate vLQ allocations (deposits and then redeem in the same frame)" in {
    val pool       = LMPool.init(frameLen = 1, epochLen = 10, epochNum = 2, programStart = 3, programBudget = 100)
    val skipFrames = 5
    val action = for {
      Right((pool1, _)) <- pool.deposit(input0)
      ctx               <- Ledger.ctx
      extendBy = (pool.conf.programStart - (ctx.height + 1)) + skipFrames * pool.conf.frameLen
      _                       <- Ledger.extendBy(extendBy)
      Right((pool2, bundle2)) <- pool1.deposit(input0)
      Right((pool3, _))       <- pool2.redeem(bundle2)
    } yield (pool2, pool3)
    val (_, (pl2, pl3)) = action.run(RuntimeCtx.init).value
    pl2.lqAllocSum shouldBe input0.value * skipFrames
    pl3.lqAllocSum shouldBe input0.value * skipFrames - input0.value
  }

  it should "return correct amount of bundled tokens on deposit (before start)" in {
    val pool = LMPool.init(frameLen = 1, epochLen = 1, epochNum = 3, programStart = 5, programBudget = 90)

    val action                   = pool.deposit(input0)
    val (_, Right((_, bundle1))) = action.run(RuntimeCtx.init).value
    bundle1 shouldBe StakingBundle(input0.value, input0.value * pool.conf.epochNum)
  }

  it should "return correct amount of bundled tokens on deposit (fractional epoch)" in {
    forAll(Gen.choose(0, pool02.conf.epochLen)) { span =>
      whenever(span >= 0) {
        val action                   = Ledger.extendBy(span) >> pool02.deposit(input0)
        val (_, Right((_, bundle1))) = action.run(RuntimeCtx.init).value
        bundle1 shouldBe StakingBundle(
          input0.value,
          input0.value * (pool02.conf.framesNum - span * pool02.conf.frameLen)
        )
      }
    }
  }

  it should "return correct pool state on deposit" in {
    val action                       = pool01.deposit(input0)
    val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.init).value
    pool1.reserves.LQ shouldBe input0.value
    pool1.reserves.vLQ shouldBe LMPool.MaxCapVLQ - bundle1.vLQ
    pool1.reserves.TMP shouldBe LMPool.MaxCapTMP - bundle1.TMP
  }

  it should "fail on premature compounding attempt" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield res
    val (_, res) = action.run(RuntimeCtx.init).value
    res shouldBe Left(LMPool.IllegalEpoch)
  }

  it should "release correct amount of reward on compounding" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      _                       <- Ledger.extendBy(2)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield (pool1, bundle1, res)
    val (_, (pool1, bundle1, Right((pool2, bundle2, output)))) = action.run(RuntimeCtx.init).value
    output shouldBe AssetOutput(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TMP = bundle1.TMP - bundle1.vLQ * pool2.conf.epochLen)
  }

  it should "deplete program budget when fully compounded (fractional epoch)" in {
    val action = for {
      Right((pool1, bundle11))    <- pool02.deposit(input1)
      _                           <- Ledger.extend
      Right((pool2, bundle21))    <- pool1.deposit(input0)
      _                           <- Ledger.extendBy(3)
      Right((pool3, bundle12, _)) <- pool2.compound(bundle11, epoch = 1)
      Right((pool4, bundle22, _)) <- pool3.compound(bundle21, epoch = 1)
      _                           <- Ledger.extendBy(2)
      Right((pool5, bundle13, _)) <- pool4.compound(bundle12, epoch = 2)
      _                           <- Ledger.extendBy(2)
      Right((pool6, bundle14, _)) <- pool5.compound(bundle22, epoch = 2)
      Right((pool7, _, _))        <- pool6.compound(bundle13, epoch = 3)
      Right((pool8, _, _))        <- pool7.compound(bundle14, epoch = 3)
    } yield pool8
    val (_, pool) = action.run(RuntimeCtx.init).value
    pool.reserves.X shouldBe 0L
  }

  it should "do not allow further deposits until prev epoch is compounded" in {
    val action = for {
      Right((pool1, bundle11)) <- pool02.deposit(input1)
      _                        <- Ledger.extendBy(pool02.conf.epochLen * pool02.conf.frameLen)
      res                      <- pool1.deposit(input0)
    } yield res
    val (_, res) = action.run(RuntimeCtx.init).value
    println(res)
  }

  it should "do nothing on an attempt to compound already fully compounded epoch" in {
    val action = for {
      Right((pool1, bundle1))         <- pool01.deposit(input0)
      _                               <- Ledger.extendBy(2)
      Right((pool2, bundle2, _))      <- pool1.compound(bundle1, epoch = 1)
      Right((pool3, bundle3, reward)) <- pool2.compound(bundle2, epoch = 1)
    } yield (pool2, pool3, bundle2, bundle3, reward)
    val (_, (pool2, pool3, bundle2, bundle3, reward)) = action.run(RuntimeCtx.init).value
    pool3 shouldBe pool2
    bundle2 shouldBe bundle3
    reward shouldBe AssetOutput(0L)
  }

  it should "do nothing on an attempt to compound on same epoch more than once" in {
    val action = for {
      Right((pool1, bundle11))         <- pool01.deposit(input0)
      Right((pool2, _))                <- pool1.deposit(input1)
      _                                <- Ledger.extendBy(2)
      Right((pool3, bundle21, _))      <- pool2.compound(bundle11, epoch = 1)
      Right((pool4, bundle31, reward)) <- pool3.compound(bundle21, epoch = 1)
    } yield (pool3, pool4, bundle21, bundle31, reward)
    val (_, (pool3, pool4, bundle21, bundle31, reward)) = action.run(RuntimeCtx.init).value
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
    val (_, (pool1, bundle1, Right((pool2, bundle2, output)))) = action.run(RuntimeCtx.init).value
    output shouldBe AssetOutput(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TMP = bundle1.TMP - bundle1.vLQ * pool2.conf.epochLen)
  }

  it should "not allow compounding in reverse order" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      _                       <- Ledger.extendBy(3)
      res                     <- pool1.compound(bundle1, epoch = 2)
    } yield res
    val (_, res) = action.run(RuntimeCtx.init).value
    res shouldBe Left(PrevEpochNotWithdrawn)
  }

  "'compound' op" should "be distributive over addition" in {
    val startHeight = 2
    val epochStep   = 2
    val action = for {
      Right((pool1, sb1)) <- pool01.deposit(input0)
      Right((pool2, sb2)) <- pool1.deposit(input1)

      _ <- Ledger.extendBy(epochStep)

      Right((pool3, _, o1)) <- pool2.compound(sb1, epoch = 1)
      Right((pool4, _, o2)) <- pool3.compound(sb2, epoch = 1)

      Right((pool4_, _, o)) <- pool2.compound(sb1 |+| sb2, epoch = 1)
    } yield (pool4, pool4_, o1, o2, o)
    val (_, (pool4, pool4_, o1, o2, o)) = action.run(RuntimeCtx.at(startHeight)).value

    pool4 shouldBe pool4_
    o1 |+| o2 shouldBe o
  }
}
