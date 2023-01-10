package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{AnyBox, BoxSim, RuntimeState, TryFromBox}

final class SwapSellBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val constants: Map[Int, Any],
  override val validatorBytes: String
) extends BoxSim[F] {

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // ===== Contract Information ===== //
      // Name: SwapSell
      // Description: Contract that validates user's swap from ERG to token in the CFMM n2t Pool.
      //
      // Constants:
      //
      // {1} -> RefundProp[ProveDlog]
      // {6} -> SpectrumIsQuote[Boolean]
      // {7} -> MaxExFee[Long]
      // {8} -> ExFeePerTokenDenom[Long]
      // {10} -> BaseAmount[Long]
      // {11} -> FeeNum[Int]
      // {13} -> PoolNFT[Coll[Byte]]
      // {14} -> RedeemerPropBytes[Coll[Byte]]
      // {15} -> QuoteId[Coll[Byte]]
      // {16} -> MinQuoteAmount[Long]
      // {20} -> ExFeePerTokenNum[Long]
      // {24} -> SpectrumId[Coll[Byte]]
      // {28} -> FeeDenom[Int]
      // {29} -> MinerPropBytes[Coll[Byte]]
      // {32} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19e50422040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040604020400010006020578060164059c01060204b0060203e404000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e20040404040404040404040404040404040404040404040404040404040404040406020320010001010602057806011606016406010004020e20030303030303030303030303030303030303030303030303030303030303030301010404060101060203e80e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d807d602db63087201d603b2a5730400d604db63087203d605b27204730500d6068c720502d6079573069d9c997e720606730773087e7309067e720606d6089c730a730bedededededed938cb27202730c0001730d93c27203730e938c720501730f92720773109573117312d801d6099973139d9c720773147315959172097316d801d60ab27204731700ed938c720a017318927e8c720a020672097319909c7e8cb27202731a00020672089c9a7207731b9a9c7ec1720106731c720890b0ada5d90109639593c27209731dc17209731e731fd90109599a8c7209018c72090273207321
      //
      // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d807d602db63087201d603b2a5730400d604db63087203d605b27204730500d6068c720502d6079573069d9c997e720606730773087e7309067e720606d6089c730a730bedededededed938cb27202730c0001730d93c27203730e938c720501730f92720773109573117312d801d6099973139d9c720773147315959172097316d801d60ab27204731700ed938c720a017318927e8c720a020672097319909c7e8cb27202731a00020672089c9a7207731b9a9c7ec1720106731c720890b0ada5d90109639593c27209731dc17209731e731fd90109599a8c7209018c72090273207321

      // Context (declarations here are only for simulations):

      val RefundProp: Boolean           = getConstant(1).get
      val SpectrumIsQuote: Boolean      = getConstant(6).get
      val MaxExFee: Long                = getConstant(7).get
      val ExFeePerTokenDenom: Long      = getConstant(8).get
      val BaseAmount: Long              = getConstant(10).get
      val FeeNum: Int                   = getConstant(11).get
      val PoolNFT: Coll[Byte]           = getConstant(13).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(14).get
      val QuoteId: Coll[Byte]           = getConstant(15).get
      val MinQuoteAmount: Long          = getConstant(16).get
      val ExFeePerTokenNum: Long        = getConstant(20).get
      val SpectrumId: Coll[Byte]        = getConstant(24).get
      val FeeDenom: Int                 = getConstant(28).get
      val MinerPropBytes: Coll[Byte]    = getConstant(29).get
      val MaxMinerFee: Long             = getConstant(32).get

      // Contract

      val poolIn     = INPUTS(0)

      // Validations
      // 1.
      val validTrade =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
          val poolNFT = poolIn.tokens(0)._1

          val poolY = poolIn.tokens(2)

          val poolReservesX = poolIn.value.toBigInt
          val poolReservesY = poolY._2.toBigInt
          val validPoolIn   = poolNFT == PoolNFT

          val rewardBox = OUTPUTS(1)

          val quoteAsset  = rewardBox.tokens(0)
          val quoteAmount =
            if (SpectrumIsQuote) {
              val deltaQuote = quoteAsset._2.toBigInt - MaxExFee
              deltaQuote * ExFeePerTokenDenom / (ExFeePerTokenDenom - ExFeePerTokenNum)
            } else {
              quoteAsset._2.toBigInt
            }
          // 1.1.
          val fairExFee   =
            if (SpectrumIsQuote) true
            else {
              val exFee     = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
              val remainder = MaxExFee - exFee
              if (remainder > 0) {
                val spectrumRem = rewardBox.tokens(1)
                spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
              } else {
                true
              }
            }

          val relaxedOutput = quoteAmount + 1 // handle rounding loss
          val base_x_feeNum = BaseAmount.toBigInt * FeeNum
          // 1.2.
          val fairPrice     = poolReservesY * base_x_feeNum <= relaxedOutput * (poolReservesX * FeeDenom + base_x_feeNum)
          // 1.3.
          val validMinerFee = OUTPUTS.map { (o: Box) =>
            if (o.propositionBytes == MinerPropBytes) o.value else 0L
          }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn &&
          rewardBox.propositionBytes == RedeemerPropBytes &&
          quoteAsset._1 == QuoteId &&
          quoteAmount >= MinQuoteAmount &&
          fairExFee &&
          fairPrice &&
          validMinerFee

        } else false

      sigmaProp(RefundProp || validTrade)
    }
}

object SwapSellBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): SwapSellBox[F]      =
    new SwapSellBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[SwapSellBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
