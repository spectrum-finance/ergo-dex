package io.ergodex.core.cfmm

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CfmmPoolContractSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {

  val testVectors = List(
    ("deposit", 995, 6861054033354L, 11951968L, 9223372028230052851L, 6862054033354L, 11953710L, 9223372028228795798L)
  )
  it should "evaluate normal deposit to true" in {
    testVectors.foreach { case (op, feeNum, rx0, ry0, rlp0, rx1, ry1, rlp1) =>
      val sim = new CfmmPoolContractSim(Long.MaxValue, feeNum, 1000, println)
      val res = sim.run(
        reservesX0  = rx0,
        reservesY0  = ry0,
        reservesLP0 = rlp0,
        reservesX1  = rx1,
        reservesY1  = ry1,
        reservesLP1 = rlp1
      )
      res shouldBe true
    }
  }
  it should "evaluate donation via deposit to true" in {
    testVectors.foreach { case (op, feeNum, rx0, ry0, rlp0, rx1, ry1, rlp1) =>
      val sim = new CfmmPoolContractSim(Long.MaxValue, feeNum, 1000, println)
      val res = sim.run(
        reservesX0  = rx0,
        reservesY0  = ry0,
        reservesLP0 = rlp0,
        reservesX1  = rx0 + 1,
        reservesY1  = ry0,
        reservesLP1 = rlp0 + 1
      )
      res shouldBe true
    }
  }
  it should "evaluate donation via swap to true" in {
    testVectors.foreach { case (op, feeNum, rx0, ry0, rlp0, rx1, ry1, rlp1) =>
      val sim = new CfmmPoolContractSim(Long.MaxValue, feeNum, 1000, println)
      val res = sim.run(
        reservesX0  = rx0,
        reservesY0  = ry0,
        reservesLP0 = rlp0,
        reservesX1  = rx0 + 1,
        reservesY1  = ry0,
        reservesLP1 = rlp0
      )
      res shouldBe true
    }
  }

  it should "evaluate illegal reserves removal to false" in {
    testVectors.foreach { case (op, feeNum, rx0, ry0, rlp0, rx1, ry1, rlp1) =>
      val sim = new CfmmPoolContractSim(Long.MaxValue, feeNum, 1000, println)
      val res = sim.run(
        reservesX0  = rx0,
        reservesY0  = ry0,
        reservesLP0 = rlp0,
        reservesX1  = rx0 - 1,
        reservesY1  = ry0,
        reservesLP1 = rlp0
      )
      res shouldBe false
    }
  }

  it should "evaluate illegal LP removal to false" in {
    testVectors.foreach { case (op, feeNum, rx0, ry0, rlp0, rx1, ry1, rlp1) =>
      val sim = new CfmmPoolContractSim(Long.MaxValue, feeNum, 1000, println)
      val res = sim.run(
        reservesX0  = rx0,
        reservesY0  = ry0,
        reservesLP0 = rlp0,
        reservesX1  = rx0 + 1,
        reservesY1  = ry0,
        reservesLP1 = rlp0 - 1
      )
      res shouldBe false
    }
  }
}
