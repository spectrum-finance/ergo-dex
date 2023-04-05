package io.ergodex.core.lqmining.simple

import io.ergodex.core.ToLedger._
import io.ergodex.core.lqmining.simple.LMPool._
import io.ergodex.core.lqmining.simple.Token._
import io.ergodex.core.lqmining.simple.TxBoxes._
import io.ergodex.core.lqmining.simple.generators.{lmConfGen, DepositGen, MultGen}
import io.ergodex.core.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.Random

class RedeemBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {
  forAll(lmConfGen) { conf =>
    forAll(DepositGen) { lq =>
      forAll(MultGen) { mults =>

        val epochLen         = conf.epochLen
        val epochNum         = conf.epochNum
        val programStart     = conf.programStart
        val redeemLimitDelta = conf.redeemLimitDelta
        val programBudget    = conf.programBudget
        val maxRoundingError = conf.maxRoundingError

        val testId = Random.nextLong()

        val pool01: LMPool[Ledger] =
          LMPool.init(
            epochLen,
            epochNum,
            programStart,
            redeemLimitDelta,
            programBudget = programBudget,
            maxRoundingError
          )

        val depositedLQAmount      = mults._1 * lq
        val input0: AssetInput[LQ] = AssetInput(depositedLQAmount)

        it should s"validate redeem behaviour before LM program start mirrored from simulation$testId" in {
          val startAtHeight                = programStart - 1
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value
          val action1                      = pool1.redeem(bundle1)
          val (_, Right((pool2, out2)))    = action1.run(RuntimeCtx.at(startAtHeight)).value

          val poolBox1               = pool1.toLedger[Ledger]
          val poolBox2               = pool2.toLedger[Ledger]
          val (userBox1, redeemBox1) = getRedeemTxBoxes(bundle1.vLQ, out2.value)

          val (_, isValid) =
            redeemBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2, userBox1))).value
          val (_, isValidPool) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value

          out2.value shouldBe depositedLQAmount
          isValid shouldBe true
          isValidPool shouldBe true
        }

        it should s"validate redeem behaviour during LM program mirrored from simulation$testId" in {
          val startAtHeight                = programStart
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value

          val action1                   = pool1.redeem(bundle1)
          val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight)).value

          val (userBox1, redeemBox1) = getRedeemTxBoxes(bundle1.vLQ, out2.value)

          val (_, isValid) =
            redeemBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(pool2.toLedger[Ledger], userBox1))).value
          out2.value shouldBe depositedLQAmount
          isValid shouldBe true
        }

        it should s"validate redeem behaviour after LM program end mirrored from simulation$testId" in {
          val startAtHeight                = programStart + 1
          val action                       = pool01.deposit(input0)
          val (_, Right((pool1, bundle1))) = action.run(RuntimeCtx.at(startAtHeight)).value
          val startAtHeight1               = programStart + epochNum * epochLen + pool1.conf.redeemLimitDelta
          val action1                      = pool1.redeem(bundle1)
          val (_, Right((pool2, out2)))    = action1.run(RuntimeCtx.at(startAtHeight1)).value
          val pool11                       = pool1.updateReserves(r => PoolReserves(r.value, r.X, r.LQ, r.vLQ, pool2.reserves.TMP))
          val (userBox1, redeemBox1)       = getRedeemTxBoxes(bundle1.vLQ, out2.value)
          val (_, isValid) =
            redeemBox1.validator
              .run(RuntimeCtx(startAtHeight1, outputs = List(pool11.toLedger[Ledger], userBox1)))
              .value
          out2.value shouldBe depositedLQAmount
          isValid shouldBe true
        }

      }
    }
  }
}
