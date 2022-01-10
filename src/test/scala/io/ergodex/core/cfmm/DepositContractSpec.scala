package io.ergodex.core.cfmm

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DepositContractSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {

  val testVectors = List(
    ("deposit", 995, 6861054033354L, 11951968L, 9223372028230052851L, 6862054033354L, 11953710L, 9223372028228795798L)
  )
  it should "evaluate normal deposit to true" in {
    val pool = CfmmPool(
      637415L,
      1981783L,
      9223372036853573813L,
      PoolConfig(
        feeNum            = 995,
        feeDenom          = 1000,
        emissionLP        = 0x7fffffffffffffffL,
        burnLP            = 1000L,
        minInitialDeposit = 1000L
      )
    )
    val depositX = 4884L
    val depositY = 11545L
    val (expectedOutputLP, change) = pool.rewardLP(depositX, depositY)
    println(s"Required change: $change")
    val sim = new DepositContractSim(pool)
    val res = sim.eval(depositX, depositY)(expectedOutputLP, change.getOrElse(0L))
    res shouldBe true
  }
}
