package io.ergodex.core.sim.lqmining.simple

import io.ergodex.core.SigmaPlatform
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sigmastate.basics.DLogProtocol.DLogProverInput

class ContractsCompiles extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with SigmaPlatform {

  property("Contracts compile") {
    val sourcePool = readSource("contracts/lqmining/simple/LMPool.sc")
    val envPool    = Map.empty[String, Any]
    printTree("Pool", sourcePool, envPool)

    val sourcePoolSelf = readSource("contracts/lqmining/simple/LMPoolSelfHosted.sc")
    val envPoolSelf    = Map.empty[String, Any]
    printTree("PoolSelf", sourcePoolSelf, envPoolSelf)

    val sourceBundle = readSource("contracts/lqmining/simple/StakingBundle.sc")
    val envBundle    = Map.empty[String, Any]
    printTree("StakingBundle", sourceBundle, envBundle)

    val sourceDeposit = readSource("contracts/lqmining/simple/Deposit.sc")
    val envDeposit = Map(
      "ExpectedNumEpochs" -> 10,
      "RedeemerProp"      -> Array.fill(32)(0: Byte),
      "BundlePropHash"    -> Array.fill(32)(1: Byte),
      "RefundPk"          -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "PoolId"            -> Array.fill(32)(2: Byte),
      "MinerPropBytes"    -> Array.fill(32)(3: Byte),
      "MaxMinerFee"       -> 100L
    )
    printTree("Deposit", sourceDeposit, envDeposit)

    val sourceRedeem = readSource("contracts/lqmining/simple/Redeem.sc")
    val envRedeem = Map(
      "ExpectedLQ"       -> 10,
      "ExpectedLQAmount" -> 1000L,
      "RedeemerProp"     -> Array.fill(32)(0: Byte),
      "RefundPk"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "MinerPropBytes"   -> Array.fill(32)(3: Byte),
      "MaxMinerFee"      -> 100L
    )
    printTree("Redeem", sourceRedeem, envRedeem)
  }
}
