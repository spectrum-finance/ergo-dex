package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.SigmaPlatform
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.util.encode.Base16
import sigmastate.Values.BooleanConstant
import sigmastate.basics.DLogProtocol.DLogProverInput

class ContractsCompiles extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with SigmaPlatform {
  property("Deposit contract compile") {
    val sourceDeposit = readSource("contracts/amm/cfmm/v3/n2t/Deposit.sc")
    val envDeposit = Map(
      "SelfXAmount"       -> 20000L,
      "SelfYAmount"       -> 30000L,
      "RefundProp"        -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "PoolNFT"           -> Array.fill(32)(2: Byte),
      "RedeemerPropBytes" -> Array.fill(32)(1: Byte),
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("Deposit", sourceDeposit, envDeposit)
  }

  property("Redeem contract compile") {
    val sourceRedeem = readSource("contracts/amm/cfmm/v3/n2t/Redeem.sc")
    val envRedeem = Map(
      "RefundProp"        -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "PoolNFT"           -> Array.fill(32)(2: Byte),
      "RedeemerPropBytes" -> Array.fill(32)(1: Byte),
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("Redeem", sourceRedeem, envRedeem)
  }

  property("SwapBuy contract compile") {
    val sourceSwapBuy = readSource("contracts/amm/cfmm/v3/n2t/SwapBuy.sc")
    val envSwapBuy = Map(
      "RefundProp"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "MaxExFee"           -> 1400L,
      "ExFeePerTokenNum"   -> 22L,
      "ExFeePerTokenDenom" -> 100L,
      "BaseAmount"         -> 1200L,
      "FeeNum"             -> 996,
      "PoolNFT"            -> Array.fill(32)(2: Byte),
      "RedeemerPropBytes"  -> Array.fill(32)(1: Byte),
      "MinQuoteAmount"     -> 800L,
      "SpectrumId"         -> Array.fill(32)(3: Byte),
      "FeeDenom"           -> 1000,
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("SwapBuy", sourceSwapBuy, envSwapBuy)
  }

  property("SwapSell contract compile") {
    val sourceSwapSell = readSource("contracts/amm/cfmm/v3/n2t/SwapSell.sc")
    val envSwapSell = Map(
      "RefundProp"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "SpectrumIsQuote"    -> BooleanConstant(true),
      "MaxExFee"           -> 1400L,
      "ExFeePerTokenNum"   -> 22L,
      "ExFeePerTokenDenom" -> 100L,
      "BaseAmount"         -> 1200L,
      "FeeNum"             -> 996,
      "QuoteId"            -> Array.fill(32)(4: Byte),
      "PoolNFT"            -> Array.fill(32)(2: Byte),
      "RedeemerPropBytes"  -> Array.fill(32)(1: Byte),
      "MinQuoteAmount"     -> 800L,
      "SpectrumId"         -> Array.fill(32)(3: Byte),
      "FeeDenom"           -> 1000,
      "MinerPropBytes" -> Base16
        .decode(
          "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304"
        )
        .get,
      "MaxMinerFee" -> 10000L
    )
    printTree("SwapSell", sourceSwapSell, envSwapSell)
  }
}
