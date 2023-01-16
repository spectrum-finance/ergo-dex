package io.ergodex.core.lqmining.simple

import io.ergodex.core.SigmaPlatform
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput

class ContractsCompiles extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with SigmaPlatform {

  property("Contracts compile") {
    val sourcePool = readSource("contracts/lqmining/simple/LMPool.sc")
    val envPool = Map(
      "BundleScriptHash" -> Base16.decode("9c9f92c4ecfb7ecce54d6cedbe5b4977532b658ce847143ec35473a6b79553ab").get
    )
    printTree("Pool", sourcePool, envPool)

    val sourcePoolSelf = readSource("contracts/lqmining/simple/LMPoolSelfHosted.sc")
    val envPoolSelf = Map(
      "BundleScriptHash" -> Base16.decode("9c9f92c4ecfb7ecce54d6cedbe5b4977532b658ce847143ec35473a6b79553ab").get
    )
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
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("Deposit", sourceDeposit, envDeposit)

    val sourceRedeem = readSource("contracts/lqmining/simple/Redeem.sc")
    val envRedeem = Map(
      "ExpectedLQ"       -> Array.fill(32)(0: Byte),
      "ExpectedLQAmount" -> 1000L,
      "RedeemerProp"     -> Array.fill(32)(1: Byte),
      "RefundPk"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("Redeem", sourceRedeem, envRedeem)
  }
}
