package io.ergodex.core.sim.lqmining.simple

import cats.syntax.semigroup._
import io.ergodex.core.sim.lqmining.simple.LMPool.PrevEpochNotWithdrawn
import io.ergodex.core.sim.lqmining.simple.Token.LQ
import io.ergodex.core.sim.{LedgerPlatform, RuntimeCtx}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tofu.syntax.monadic._

class LqMiningPoolSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  val maxRoundingError = 1000L
  val KK               = 10000L * maxRoundingError
  val epochLen         = 11
  val epochNum         = 3
  val redeemDelta      = 10
  val programStart     = 100

  val pool01: LMPool[Ledger] =
    LMPool.init(
      epochLen,
      epochNum,
      programStart,
      redeemDelta,
      programBudget = 5000000 * maxRoundingError,
      maxRoundingError
    )
  val pool02: LMPool[Ledger] =
    LMPool.init(
      epochLen,
      epochNum,
      programStart,
      redeemDelta,
      programBudget = 900000L * maxRoundingError,
      maxRoundingError
    )

  val input0: AssetInput[LQ] = AssetInput(1 * KK)
  val input1: AssetInput[LQ] = AssetInput(2 * KK)
  val input2: AssetInput[LQ] = AssetInput(4 * KK)
  val input3: AssetInput[LQ] = AssetInput(3 * KK)
  val input4: AssetInput[LQ] = AssetInput(2 * KK)

  it should "return correct amount of bundled tokens on deposit" in {
    forAll(Gen.choose(0, 1)) { span =>
      whenever(span >= 0) {
        val action                   = Ledger.extendBy(span * epochLen) >> pool01.deposit(input0)
        val (_, Right((_, bundle1))) = action.run(RuntimeCtx.at(programStart - 1)).value
        bundle1 shouldBe StakingBundle(input0.value, input0.value * (pool01.conf.epochNum - span))
      }
    }
  }

  it should "return correct amount of bundled tokens on deposit (before start)" in {
    val action                   = pool01.deposit(input0)
    val startAtHeight            = programStart - 1
    val (_, Right((_, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value
    bundle1 shouldBe StakingBundle(input0.value, input0.value * pool01.conf.epochNum)
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
      _                       <- Ledger.extendBy(epochLen + 1)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield (pool1, bundle1, res)
    val (_, (pool1, bundle1, Right((pool2, bundle2, output)))) =
      action.run(RuntimeCtx.at(programStart - 1)).value
    output shouldBe AssetOutput(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TMP = bundle1.TMP - bundle1.vLQ * 1)
  }

  it should "do not allow further deposits until prev epoch is compounded" in {
    val action = for {
      Right((pool1, _)) <- pool02.deposit(input1)
      _                 <- Ledger.extendBy(pool02.conf.epochLen)
      res               <- pool1.deposit(input0)
    } yield res
    val (_, res) = action.run(RuntimeCtx.at(programStart)).value
  }

  it should "do nothing on an attempt to compound already fully compounded epoch" in {
    val action = for {
      Right((pool1, bundle1))         <- pool01.deposit(input0)
      _                               <- Ledger.extendBy(epochLen + 1)
      Right((pool2, bundle2, _))      <- pool1.compound(bundle1, epoch = 1)
      Right((pool3, bundle3, reward)) <- pool2.compound(bundle2, epoch = 1)
    } yield (pool2, pool3, bundle2, bundle3, reward)
    val (_, (pool2, pool3, bundle2, bundle3, reward)) = action.run(RuntimeCtx.at(programStart - 1)).value
    pool3 shouldBe pool2
    bundle2 shouldBe bundle3
    reward shouldBe AssetOutput(0L)
  }

  it should "do nothing on an attempt to compound on same epoch more than once" in {
    val action = for {
      Right((pool1, bundle11))         <- pool01.deposit(input0)
      Right((pool2, _))                <- pool1.deposit(input1)
      _                                <- Ledger.extendBy(epochLen + 1)
      Right((pool3, bundle21, _))      <- pool2.compound(bundle11, epoch = 1)
      Right((pool4, bundle31, reward)) <- pool3.compound(bundle21, epoch = 1)
    } yield (pool3, pool4, bundle21, bundle31, reward)
    val (_, (pool3, pool4, bundle21, bundle31, reward)) = action.run(RuntimeCtx.at(programStart - 1)).value
    pool4 shouldBe pool3
    bundle31 shouldBe bundle21
    reward shouldBe AssetOutput(0L)
  }

  it should "allow late compounding" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      _                       <- Ledger.extendBy(3 * epochLen)
      res                     <- pool1.compound(bundle1, epoch = 1)
    } yield (pool1, bundle1, res)
    val (_, (pool1, bundle1, Right((pool2, bundle2, output)))) = action.run(RuntimeCtx.at(programStart - 1)).value
    output shouldBe AssetOutput(pool1.conf.epochAlloc)
    bundle2 shouldBe bundle1.copy(TMP = bundle1.TMP - bundle1.vLQ * 1)
  }

  it should "not allow compounding in reverse order" in {
    val action = for {
      Right((pool1, bundle1)) <- pool01.deposit(input0)
      _                       <- Ledger.extendBy(3 * epochLen)
      res                     <- pool1.compound(bundle1, epoch = 2)
    } yield res
    val (_, res) = action.run(RuntimeCtx.at(programStart - 1)).value
    res shouldBe Left(PrevEpochNotWithdrawn)
  }

  it should "deplete program budget when fully compounded" in {
    val action = for {
      Right((pool1, bundle11))    <- pool01.deposit(input0)
      Right((pool2, bundle21))    <- pool1.deposit(input1)
      _                           <- Ledger.extendBy(epochLen + 1)
      Right((pool3, bundle12, _)) <- pool2.compound(bundle11, epoch = 1)
      Right((pool4, bundle22, _)) <- pool3.compound(bundle21, epoch = 1)

      _                           <- Ledger.extendBy(epochLen)
      Right((pool5, bundle13, _)) <- pool4.compound(bundle12, epoch = 2)
      Right((pool6, bundle23, _)) <- pool5.compound(bundle22, epoch = 2)
      Right((pool7, _))           <- pool6.redeem(bundle23)
      _                           <- Ledger.extendBy(epochLen)
      Right((pool8, _, _))        <- pool7.compound(bundle13, epoch = 3)

    } yield pool8
    val (_, pool) = action.run(RuntimeCtx.at(programStart - 1)).value
    (0 <= pool.reserves.X) && (pool.reserves.X <= maxRoundingError) shouldBe true
  }

  it should "deplete program budget when fully compounded with redeem after program end" in {
    val action = for {
      Right((pool1, bundle11))    <- pool01.deposit(input0)
      Right((pool2, bundle21))    <- pool1.deposit(input1)
      Right((pool3, bundle31))    <- pool2.deposit(input2)
      _                           <- Ledger.extendBy(epochLen + 1)
      Right((pool4, bundle12, _)) <- pool3.compound(bundle11, epoch = 1)
      Right((pool5, bundle22, _)) <- pool4.compound(bundle21, epoch = 1)
      Right((pool6, bundle32, _)) <- pool5.compound(bundle31, epoch = 1)

      _                           <- Ledger.extendBy(epochLen)
      Right((pool7, bundle13, _)) <- pool6.compound(bundle12, epoch = 2)
      _                           <- Ledger.extendBy(epochLen)
      Right((pool8, bundle23, _)) <- pool7.compound(bundle22, epoch = 2)
      Right((pool9, bundle33, _)) <- pool8.compound(bundle32, epoch = 2)

      Right((pool10, bundle14, _)) <- pool9.compound(bundle13, epoch = 3)
      Right((pool11, bundle24, _)) <- pool10.compound(bundle23, epoch = 3)
      Right((pool12, bundle34, _)) <- pool11.compound(bundle33, epoch = 3)
      _                            <- Ledger.extendBy(10 * epochLen)
      Right((pool13, _))           <- pool12.redeem(bundle14)
      Right((pool14, _))           <- pool13.redeem(bundle24)
      Right((pool15, _))           <- pool14.redeem(bundle34)

    } yield pool15
    val (_, pool) = action.run(RuntimeCtx.at(programStart - 1)).value
    pool.reserves.LQ shouldBe 0L
    (0 <= pool.reserves.X) && (pool.reserves.X <= maxRoundingError) shouldBe true
  }

  it should "compound op should be distributive over addition" in {
    val action = for {
      Right((pool1, sb1)) <- pool01.deposit(input0)
      Right((pool2, sb2)) <- pool1.deposit(input1)

      _ <- Ledger.extendBy(epochLen + 1)

      Right((pool3, _, o1)) <- pool2.compound(sb1, epoch = 1)
      Right((pool4, _, o2)) <- pool3.compound(sb2, epoch = 1)

      Right((pool4_, _, o)) <- pool2.compound(sb1 |+| sb2, epoch = 1)
    } yield (pool4, pool4_, o1, o2, o)
    val (_, (pool4, pool4_, o1, o2, o)) = action.run(RuntimeCtx.at(programStart - 1)).value

    pool4 shouldBe pool4_
    o1 |+| o2 shouldBe o
  }
}
