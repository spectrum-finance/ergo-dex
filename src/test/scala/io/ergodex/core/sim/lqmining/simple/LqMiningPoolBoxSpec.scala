package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.sim.Helpers.{boxId, tokenId}
import io.ergodex.core.sim.ToLedger._
import io.ergodex.core.sim.lqmining.simple.LMPool._
import io.ergodex.core.sim.lqmining.simple.Token._
import io.ergodex.core.sim.{LedgerPlatform, RuntimeCtx}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks


class LqMiningPoolBoxSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks with LedgerPlatform {

  val epochNum = 3
  val epochLen = 5
  val programStart = 20000
  val redeemDelta = 10
  val maxRoundingError = 1000L
  val epochReg = 8

  val pool01: LMPool[Ledger] = {
    LMPool.init(epochLen, epochNum, programStart, redeemDelta, programBudget = 9000L * maxRoundingError, maxRoundingError)
  }
  val input0: AssetInput[LQ] = AssetInput(1 * maxRoundingError)
  val input1: AssetInput[LQ] = AssetInput(2 * maxRoundingError)

  def epochIx(ctx: RuntimeCtx, conf: LMConfig): Int = {
    val curBlockIx = ctx.height - conf.programStart + 1
    val curEpochIxRem = curBlockIx % conf.epochLen
    val curEpochIxR = curBlockIx / conf.epochLen
    val curEpochIx = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR
    curEpochIx
  }

  it should "validate deposit behaviour during LM program mirrored from simulation" in {
    val startAtHeight = programStart + 1
    val action = pool01.deposit(input0)
    val currEpoch = epochIx(RuntimeCtx.at(startAtHeight), pool01.conf)
    val (_, Right((pool1, _))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox0 = pool01.toLedger[Ledger].setRegister(epochReg, currEpoch)
    val poolBox1 = pool1.toLedger[Ledger].setRegister(epochReg, currEpoch)
    val (_, isValid) = poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1))).value
    isValid shouldBe true
  }

  it should "validate deposit behaviour during LM program mirrored from simulation during compounding" in {
    val startAtHeight = programStart - 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val action1 = pool1.deposit(input0)
    val (_, Right((pool2, sb2))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val action2 = pool2.compound(sb1, epoch = 1)
    val (_, Right((pool3, _, w))) = action2.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    val action3 = pool3.deposit(input0)
    val (_, Left(pool4)) = action3.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    pool4 shouldBe PrevEpochNotWithdrawn
  }

  it should "validate deposit behaviour after LM program end mirrored from simulation" in {
    val startAtHeight = programStart + epochNum * epochLen + 1
    val action = pool01.deposit(input0)
    val (_, Left(pool1)) = action.run(RuntimeCtx.at(startAtHeight)).value
    pool1 shouldBe ProgramEnded
  }

  it should "validate compound behaviour after first epoch mirrored from simulation" in {
    val startAtHeight = programStart - 1
    val epochStep = epochLen + 1
    val action = for {
      Right((pool1, sb1)) <- pool01.deposit(input0)
      Right((pool2, sb2)) <- pool1.deposit(input1)
      - <- Ledger.extendBy(epochStep)
      Right((pool3, _, _)) <- pool2.compound(sb1, epoch = 1)
      Right((pool4, _, _)) <- pool3.compound(sb2, epoch = 1)
    } yield (pool2, pool3, pool4)

    val (_, (pool2, pool3, pool4)) = action.run(RuntimeCtx.at(startAtHeight)).value

    val poolBox2 = pool2.toLedger[Ledger].setRegister(epochReg, 1)
    val poolBox3 = pool3.toLedger[Ledger].setRegister(epochReg, 1)
    val poolBox4 = pool4.toLedger[Ledger].setRegister(epochReg, 1)

    val (_, isValidFirstCompounding) =
      poolBox2.validator.run(RuntimeCtx(startAtHeight + epochStep, outputs = List(poolBox3))).value

    val (_, isValidSecondCompounding) =
      poolBox3.validator.run(RuntimeCtx(startAtHeight + epochStep, outputs = List(poolBox4))).value

    isValidFirstCompounding shouldBe true
    isValidSecondCompounding shouldBe true
  }

  it should "validate illegal compound behaviour mirrored from simulation" in {
    val startAtHeight = epochLen * 2
    val action = pool01.deposit(input0)
    val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox0 = pool1.toLedger[Ledger]
    val action1 = pool1.compound(sb1, epoch = 2)
    val (_, Left(pool2)) = action1.run(RuntimeCtx.at(startAtHeight)).value

    val reward = pool01.conf.programBudget / epochNum
    val X1 = pool01.reserves.X - reward
    val TMP1 = pool01.reserves.TMP + sb1.TMP

    val poolBox1 = new LqMiningPoolBox(
      boxId("LM_Pool_NFT_ID"),
      MinCollateralErg,
      DefaultCreationHeight,
      tokens = Vector(
        tokenId("LM_Pool_NFT_ID") -> 1L,
        tokenId("X") -> X1,
        tokenId("LQ") -> pool01.reserves.LQ,
        tokenId("vLQ") -> pool01.reserves.vLQ,
        tokenId("TMP") -> TMP1
      ),
      registers = Map(
        4 -> Vector(pool01.conf),
        5 -> pool01.conf.programBudget,
        6 -> pool01.conf.MaxRoundingError,
        7 -> pool01.execution.execBudget,
      )
    )

    val (_, isValid) =
      poolBox0.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox1.setRegister(epochReg, 2)))).value

    pool2 shouldBe IllegalEpoch
    isValid shouldBe false

  }

  it should "validate redeem behaviour before LM program start mirrored from simulation" in {
    val startAtHeight = programStart - 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox1 = pool1.toLedger[Ledger]
    val action1 = pool1.redeem(sb1)
    val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox2 = pool2.toLedger[Ledger]
    val (_, isValid) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
    out2.value shouldBe input0.value
    isValid shouldBe true
  }

  it should "validate redeem behaviour during LM program mirrored from simulation" in {
    val startAtHeight = programStart + 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox1 = pool1.toLedger[Ledger]
    val action1 = pool1.redeem(sb1)
    val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox2 = pool2.toLedger[Ledger]
    val (_, isValid) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
    out2.value shouldBe input0.value
    isValid shouldBe true
  }

  it should "validate redeem behaviour during compounding mirrored from simulation" in {
    val startAtHeight = programStart - 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, bundle11))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val action1 = pool1.deposit(input1)
    val (_, Right((pool2, bundle21))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val action3 = pool2.compound(bundle11, epoch = 1)
    val (_, Right((pool3, bundle12, rew11))) = action3.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    val action4 = pool3.compound(bundle21, epoch = 1)
    val (_, Right((pool4, bundle22, rew12))) = action4.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    val action5 = pool4.redeem(bundle22)
    val (_, Right((pool5, out))) = action5.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    val action6 = pool4.redeem(bundle22)
    val (_, Left(err)) = action6.run(RuntimeCtx.at(startAtHeight + 3 * epochLen + 1)).value
    out.value shouldBe input1.value
    err shouldBe PrevEpochNotWithdrawn
  }

  it should "validate redeem behaviour (2) during compounding mirrored from simulation" in {
    val startAtHeight = programStart - 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, bundle11))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val action1 = pool1.deposit(input1)
    val (_, Right((pool2, bundle21))) = action1.run(RuntimeCtx.at(startAtHeight)).value
    val action3 = pool2.compound(bundle11, epoch = 1)
    val (_, Right((pool3, bundle12, rew11))) = action3.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    val action4 = pool3.redeem(bundle21)
    val (_, Left(err)) = action4.run(RuntimeCtx.at(startAtHeight + epochLen + 1)).value
    err shouldBe PrevEpochNotWithdrawn
  }

  it should "validate redeem behaviour after LM program end mirrored from simulation" in {
    val startAtHeight = programStart + 1
    val action = pool01.deposit(input0)
    val (_, Right((pool1, sb1))) = action.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox1 = pool1.toLedger[Ledger]
    val action1 = pool1.redeem(sb1)
    val startAtHeight1 = programStart + epochNum * epochLen + pool01.conf.redeemLimitDelta
    val (_, Right((pool2, out2))) = action1.run(RuntimeCtx.at(startAtHeight1)).value
    val poolBox2 = pool2.toLedger[Ledger]
    val (_, isValid) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
    out2.value shouldBe input0.value
    isValid shouldBe true
  }

  it should "validate increase execution budget behaviour mirrored from simulation" in {
    val startAtHeight = programStart + 1
    val action = pool01.increaseExecutionBudget(1000)
    val (_, Right(pool1)) = action.run(RuntimeCtx.at(startAtHeight)).value
    val poolBox1 = pool01.toLedger[Ledger]
    val poolBox2 = pool1.toLedger[Ledger]
    val (_, isValid) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
    isValid shouldBe true
  }

  it should "prevent creation height decreasing" in {
    val startAtHeight = programStart + 1
    val poolBox1 = pool01.toLedger[Ledger]
    val poolBox2 = new LqMiningPoolBox(
      boxId("LM_Pool_NFT_ID"),
      pool01.reserves.value,
      0,
      tokens = Vector(
        tokenId("LM_Pool_NFT_ID") -> 1L,
        tokenId("X") -> pool01.reserves.X,
        tokenId("LQ") -> 10,
        tokenId("vLQ") -> pool01.reserves.vLQ,
        tokenId("TMP") -> pool01.reserves.TMP
      ),
      registers = Map(
        4 -> Vector(pool01.conf),
        5 -> pool01.conf.programBudget,
        6 -> pool01.conf.MaxRoundingError,
        7 -> pool01.execution.execBudget,
      )
    )
    val (_, isValid) = poolBox1.validator.run(RuntimeCtx(startAtHeight, outputs = List(poolBox2))).value
    isValid shouldBe false
  }
}
