package io.ergodex.core.lqmining.extended

import io.ergodex.core.SigmaPlatform
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput

class ContractsCompiles extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with SigmaPlatform {

  property("Contracts compile") {
    val sourcePoolSelf = readSource("contracts/lqmining/extended/LMPoolSelfHosted.sc")
    val envPoolSelf = Map(
      "BundleScriptHash" -> Base16.decode("7dea0cba5791f44220d3ecf272c137a233c5b5060be2e6dcf04e647219e20e57").get
    )
    printTree("PoolSelf", sourcePoolSelf, envPoolSelf)


    val sourceStakingBundle = readSource("contracts/lqmining/extended/StakingBundle.sc")
    val envStakingBundle = Map(
      "actionId" -> 11,
    )
    printTree("StakingBundle", sourceStakingBundle, envStakingBundle)


    val sourceDeposit = readSource("contracts/lqmining/extended/Deposit.sc")
    val envDeposit = Map(
      "ExpectedNumEpochs" -> 10,
      "RedeemerProp"      -> Array.fill(32)(0: Byte),
      "BundlePropHash"    -> Base16.decode("7dea0cba5791f44220d3ecf272c137a233c5b5060be2e6dcf04e647219e20e57").get,
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

    val sourceRedeem = readSource("contracts/lqmining/extended/Redeem.sc")
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


    val sourceDepositBudget = readSource("contracts/lqmining/extended/DepositBudget.sc")
    val envDepositBudget = Map(
      "RedeemerProp"      -> Array.fill(32)(0: Byte),
      "RefundPk"          -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "PoolId"            -> Array.fill(32)(2: Byte),
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("DepositBudget", sourceDepositBudget, envDepositBudget)


    val sourceRedeemBudget = readSource("contracts/lqmining/extended/RedeemBudget.sc")
    val envRedeemBudget = Map(
      "ExpectedBudget"       -> Array.fill(32)(0: Byte),
      "ExpectedBudgetAmount" -> 1000L,
      "RedeemerProp"     -> Array.fill(32)(1: Byte),
      "RefundPk"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("RedeemBudget", sourceRedeemBudget, envRedeemBudget)


    val sourceRedeemRewards = readSource("contracts/lqmining/extended/RedeemRewards.sc")
    val envRedeemRewards = Map(
      "ExpectedReward"       -> Array.fill(32)(0: Byte),
      "ExpectedRewardAmount" -> 1000L,
      "RedeemerProp"     -> Array.fill(32)(1: Byte),
      "RefundPk"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("RedeemRewards", sourceRedeemRewards, envRedeemRewards)
  }
}
