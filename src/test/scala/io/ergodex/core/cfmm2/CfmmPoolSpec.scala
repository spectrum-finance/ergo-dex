package io.ergodex.core.cfmm2

import io.ergodex.core.cfmm2.generators._
import org.scalatest.flatspec._
import org.scalatest.matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CfmmPoolSpec extends AnyFlatSpec with should.Matchers with ScalaCheckPropertyChecks {

  it should "support minimal amount swap [X]" in {
    forAll(poolGen) { pool0 =>
      val optMinAmountY = pool0.getSoldY(1L)
      whenever(optMinAmountY.isDefined) {
        optMinAmountY.map { tryToBuyY =>
          tryToBuyY shouldBe pool0.inputAmount(token = "x", output = 1L)
          whenever(tryToBuyY < pool0.y) {
            val optSoldX = pool0.getSoldX(tryToBuyY)
            whenever(optSoldX.isDefined) {
              optSoldX.map { soldX =>
                val boughtY  = pool0.getBoughtY(soldX)
                soldX shouldBe pool0.inputAmount("y", tryToBuyY)
                boughtY shouldBe Some(pool0.outputAmount("x", soldX))
                val optPool1 = pool0.sellX(soldX)
                optPool1 shouldBe defined
                optPool1.foreach { pool1 =>
                  Some(pool0.y - pool1.y) shouldBe boughtY
                  pool0.y - pool1.y should be >= tryToBuyY // actual amount bought must be >= whatever we want to buy
                  pool1.x should be > pool0.x
                  pool1.y should be < pool0.y // non zero swap needed
                  pool0.x * pool0.y should be < pool1.x * pool1.y
                }
              }
            }
          }
        }
      }
    }
  }

  it should "support minimal amount swap [Y]" in {
    forAll(poolGen) { pool0 =>
      val optMinAmountX = pool0.getSoldX(1L)
      whenever(optMinAmountX.isDefined) {
        optMinAmountX.map { tryToBuyX =>
          whenever(tryToBuyX < pool0.x) {
            val optSoldY = pool0.getSoldY(tryToBuyX)
            whenever(optSoldY.isDefined) {
              optSoldY.map { soldY =>
                val boughtX  = pool0.getBoughtX(soldY)
                soldY shouldBe pool0.inputAmount("x", tryToBuyX)
                boughtX shouldBe Some(pool0.outputAmount("y", soldY))
                val optPool1 = pool0.sellY(soldY)
                optPool1 shouldBe defined
                optPool1.foreach { pool1 =>
                  Some(pool0.x - pool1.x) shouldBe boughtX
                  pool0.x - pool1.x should be >= tryToBuyX // actual amount bought must be >= whatever we want to buy
                  pool1.y should be > pool0.y
                  pool1.x should be < pool0.x // non zero swap needed
                  pool0.x * pool0.y should be < pool1.x * pool1.y
                }
              }
            }
          }
        }
      }
    }
  }

  it should "support full liquidation" in {
    forAll(poolGen) { pool0 =>
      forAll(depositSellGen) { actions =>
        val pool = actions.foldLeft(pool0) { case (pool, op) =>
          (op match {
            case Deposit(x, y) => pool.deposit(x, y)
            case Redeem(lp)    => pool.redeem(lp)
            case SellX(amount) => pool.sellX(amount)
            case SellY(amount) => pool.sellY(amount)
          }).getOrElse(pool)
        }

        val poolDepleted = if (pool.supplyLP > 0) pool.redeem(pool.supplyLP).getOrElse(pool) else pool
        poolDepleted.x should be(0)
        poolDepleted.y should be(0)
        poolDepleted.supplyLP should be(0)
      }
    }
  }
}
