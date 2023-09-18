package io.ergodex.core.lqmining.parallel

import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.parallel.TxBoxes._
import io.ergodex.core.lqmining.parallel.generators.lmConfGen
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class RedeemBudgetBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  forAll(lmConfGen) { conf =>
    val epochLen         = conf.epochLen
    val epochNum         = conf.epochNum
    val programStart     = conf.programStart
    val redeemLimitDelta = conf.redeemLimitDelta
    val maxRoundingError = conf.maxRoundingError
    val mainBudget       = conf.mainBudget
    val optBudget        = conf.optBudget

    val testId = Random.nextLong()

    val pool01: LMPool[Ledger] =
      LMPool.init(
        epochLen,
        epochNum,
        mainBudget,
        optBudget,
        programStart,
        redeemLimitDelta,
        maxRoundingError
      )

    it should s"validate simple redeem budget behaviour$testId" in {
      val startAtHeight             = programStart + epochNum * epochLen + redeemLimitDelta
      val action                    = pool01.redeemBudget(0)
      val (_, Right((pool1, out1))) = action.run(RuntimeCtx.at(startAtHeight)).value
      val action1                   = pool1.redeemBudget(1)
      val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight)).value

      val poolBox0 = pool01.toLedger[Ledger]

      val poolBox1 = pool1.toLedger[Ledger]
      val poolBox2 = pool2.toLedger[Ledger]

      val (redeemBudgetBox0, userBox0) =
        getRedeemBudgetBoxes("X0", mainBudget, out1.value, "Host", "Host")

      val (_, isValidPool0) = poolBox0.validator
        .run(
          RuntimeCtx(
            startAtHeight,
            inputs  = List(poolBox0, redeemBudgetBox0),
            outputs = List(poolBox1, userBox0)
          )
        )
        .value

      val (_, isValidBudgetRedeem0) = redeemBudgetBox0.validator
        .run(
          RuntimeCtx(
            startAtHeight,
            inputs  = List(poolBox0, redeemBudgetBox0),
            outputs = List(poolBox1, userBox0)
          )
        )
        .value

      val (redeemBudgetBox1, userBox1) =
        getRedeemBudgetBoxes("X1", optBudget, out2.value, "Spectrum", "Spectrum")

      val (_, isValidPool1) = poolBox1.validator
        .run(
          RuntimeCtx(
            startAtHeight,
            inputs  = List(poolBox1, redeemBudgetBox1),
            outputs = List(poolBox2, userBox1)
          )
        )
        .value

      val (_, isValidBudgetRedeem1) = redeemBudgetBox1.validator
        .run(
          RuntimeCtx(
            startAtHeight,
            inputs  = List(poolBox1, redeemBudgetBox1),
            outputs = List(poolBox2, userBox1)
          )
        )
        .value

      out1.value == mainBudget shouldBe true
      out2.value == optBudget shouldBe true

      isValidPool0 shouldBe true
      isValidBudgetRedeem0 shouldBe true
      pool1.reserves.X0 == 0 shouldBe true
      pool1.reserves.X1 != 0 shouldBe true

      val shouldBeValue = if (out1.value > 0) true else false
      isValidBudgetRedeem1 shouldBe shouldBeValue
      pool2.reserves.X0 == 0 shouldBe true
      pool2.reserves.X1 == 0 shouldBe true

    }

    it should s"validate illegal redeemer redeem budget behaviour$testId" in {
      val startAtHeight             = programStart + epochNum * epochLen + redeemLimitDelta
      val action                    = pool01.redeemBudget(0)
      val (_, Right((pool1, out1))) = action.run(RuntimeCtx.at(startAtHeight)).value

      val poolBox0 = pool01.toLedger[Ledger]

      val poolBox1 = pool1.toLedger[Ledger]

      val (redeemBudgetBox0, userBox0) =
        getRedeemBudgetBoxes("X0", mainBudget, out1.value, "Badman", "Host")

      val (_, isValidPool0) = poolBox0.validator
        .run(
          RuntimeCtx(
            startAtHeight,
            inputs  = List(poolBox0, redeemBudgetBox0),
            outputs = List(poolBox1, userBox0)
          )
        )
        .value

      val (_, isValidBudgetRedeem0) = redeemBudgetBox0.validator
        .run(
          RuntimeCtx(
            startAtHeight,
            inputs  = List(poolBox0, redeemBudgetBox0),
            outputs = List(poolBox1, userBox0)
          )
        )
        .value

      isValidPool0 shouldBe false
      isValidBudgetRedeem0 shouldBe false
    }

    it should s"validate illegal time redeem budget behaviour$testId" in {
      val startAtHeight             = programStart + epochNum * epochLen + redeemLimitDelta
      val action                    = pool01.redeemBudget(0)
      val (_, Right((pool1, out1))) = action.run(RuntimeCtx.at(startAtHeight)).value
      val poolBox0                  = pool01.toLedger[Ledger]

      val poolBox1 = pool1.toLedger[Ledger]

      val (redeemBudgetBox0, userBox0) =
        getRedeemBudgetBoxes("X0", mainBudget, out1.value, "Badman", "Host")

      val (_, isValidPool0) = poolBox0.validator
        .run(
          RuntimeCtx(
            programStart,
            inputs  = List(poolBox0, redeemBudgetBox0),
            outputs = List(poolBox1, userBox0)
          )
        )
        .value

      val (_, isValidBudgetRedeem0) = redeemBudgetBox0.validator
        .run(
          RuntimeCtx(
            programStart,
            inputs  = List(poolBox0, redeemBudgetBox0),
            outputs = List(poolBox1, userBox0)
          )
        )
        .value

      isValidPool0 shouldBe false
      isValidBudgetRedeem0 shouldBe false
    }
  }
}
