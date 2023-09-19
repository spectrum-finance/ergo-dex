package io.ergodex.core.lqmining.parallel

import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.parallel.TxBoxes._
import io.ergodex.core.lqmining.parallel.generators.{DepositGen, lmConfGen}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class DepositBudgetBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  forAll(lmConfGen) { conf =>
    forAll(DepositGen) { budget =>
      val epochLen         = conf.epochLen
      val epochNum         = conf.epochNum
      val programStart     = conf.programStart
      val redeemLimitDelta = conf.redeemLimitDelta
      val mainBudget       = conf.mainBudget
      val optBudget        = conf.optBudget
      val maxRoundingError = conf.maxRoundingError

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

      val depositedMain = budget
      val depositedOpt  = budget

      it should s"validate simple deposit budget behaviour$testId" in {
        val startAtHeight       = programStart - 1
        val action              = pool01.depositBudget(depositedMain, 0)
        val (_, Right((pool1))) = action.run(RuntimeCtx.at(startAtHeight)).value
        val action1             = pool1.depositBudget(depositedOpt, 1)
        val (_, Right((pool2))) = action1.run(RuntimeCtx.at(startAtHeight)).value

        val poolBox0 = pool01.toLedger[Ledger]

        val poolBox1 = pool1.toLedger[Ledger]
        val poolBox2 = pool2.toLedger[Ledger]

        val depositBudgetBox0 =
          getDepositBudgetBox("X0", depositedMain, "Host", 1)

        val (_, isValidPool0) = poolBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        val (_, isValidBudgetDeposit0) = depositBudgetBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        val depositBudgetBox1 =
          getDepositBudgetBox("X1", depositedOpt, "Spectrum", 5)

        val (_, isValidPool1) = poolBox1.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox1, depositBudgetBox1),
              outputs = List(poolBox2)
            )
          )
          .value

        val (_, isValidBudgetDeposit1) = depositBudgetBox1.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox1, depositBudgetBox1),
              outputs = List(poolBox2)
            )
          )
          .value

        isValidPool0 shouldBe true
        isValidBudgetDeposit0 shouldBe true

        isValidPool1 shouldBe true
        isValidBudgetDeposit1 shouldBe true
      }

      it should s"validate invalid deposit budget amount behaviour$testId" in {
        val startAtHeight       = programStart - 1
        val action              = pool01.depositBudget(depositedMain, 0)
        val (_, Right((pool1))) = action.run(RuntimeCtx.at(startAtHeight)).value

        val poolBox0 = pool01.toLedger[Ledger]

        val poolBox1 = pool1.toLedger[Ledger]

        val depositBudgetBox0 =
          getDepositBudgetBox("X0", depositedMain + 1, "Host", 1)

        val (_, isValidPool0) = poolBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        val (_, isValidBudgetDeposit0) = depositBudgetBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        isValidPool0 shouldBe true
        isValidBudgetDeposit0 shouldBe false
      }

      it should s"validate invalid deposit budget redeemer behaviour$testId" in {
        val startAtHeight       = programStart - 1
        val action              = pool01.depositBudget(depositedMain, 0)
        val (_, Right((pool1))) = action.run(RuntimeCtx.at(startAtHeight)).value

        val poolBox0 = pool01.toLedger[Ledger]

        val poolBox1 = pool1.toLedger[Ledger]

        val depositBudgetBox0 =
          getDepositBudgetBox("X0", depositedMain, "Badman", 1)

        val (_, isValidPool0) = poolBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        val (_, isValidBudgetDeposit0) = depositBudgetBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        isValidPool0 shouldBe true
        isValidBudgetDeposit0 shouldBe false
      }

      it should s"validate invalid deposit budget token behaviour$testId" in {
        val startAtHeight       = programStart - 1
        val action              = pool01.depositBudget(depositedMain, 0)
        val (_, Right((pool1))) = action.run(RuntimeCtx.at(startAtHeight)).value

        val poolBox0 = pool01.toLedger[Ledger]

        val poolBox1 = pool1.toLedger[Ledger]

        val depositBudgetBox0 =
          getDepositBudgetBox("X1", depositedMain, "Host", 1)

        val (_, isValidPool0) = poolBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        val (_, isValidBudgetDeposit0) = depositBudgetBox0.validator
          .run(
            RuntimeCtx(
              startAtHeight,
              inputs  = List(poolBox0, depositBudgetBox0),
              outputs = List(poolBox1)
            )
          )
          .value

        isValidPool0 shouldBe true
        isValidBudgetDeposit0 shouldBe false
      }
    }
  }
}
