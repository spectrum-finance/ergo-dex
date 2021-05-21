package io.ergodex.core.cfmm

import io.ergodex.core.cfmm.generators._
import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CfmmPoolSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {

  "A CFMM Pool" should "support full liquidation" in {
    forAll(cfmmConfGen) { conf =>
      forAll(initialDepositGen(conf.minInitialDeposit)) { case (ix, iy) =>
        whenever(ix >= conf.minInitialDeposit && iy >= conf.minInitialDeposit) {
          val pool0 = CfmmPool.init(ix, iy, conf)
          forAll(depositSwapGen) { actions =>
            val pool         = actions.foldLeft(pool0) { case (pool, op) =>
              op match {
                case Deposit(x, y)       => pool.deposit(x, y)
                case Redeem(lp)          => pool.redeem(lp)
                case Swap(asset, amount) => pool.swap(asset, amount)
              }
            }
            val poolDepleted = pool.redeem(pool.supplyLP)
            poolDepleted.x should be(0)
            poolDepleted.y should be(0)
            poolDepleted.lp should be(conf.emissionLP)
          }
        }
      }
    }
  }
}
