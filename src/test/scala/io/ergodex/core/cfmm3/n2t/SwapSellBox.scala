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
      // {1} -> MaxExFee[Long]
      // {2} -> ExFeePerTokenDenom[Long]
      // {3} -> BaseAmount[Long]
      // {4} -> FeeNum[Int]
      // {5} -> RefundProp[ProveDlog]
      // {10} -> SpectrumIsQuote[Boolean]
      // {13} -> PoolNFT[Coll[Byte]]
      // {14} -> RedeemerPropBytes[Coll[Byte]]
      // {15} -> QuoteId[Coll[Byte]]
      // {16} -> MinQuoteAmount[Long]
      // {19} -> ExFeePerTokenNum[Long]
      // {22} -> SpectrumId[Coll[Byte]]
      // {26} -> FeeDenom[Int]
      // {27} -> MinerPropBytes[Coll[Byte]]
      // {30} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19f40420040005f01505c80105e01204c80f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040204000101059c0104000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e20040404040404040404040404040404040404040404040404040404040404040405c00c01010101052c06010004020e2003030303030303030303030303030303030303030303030303030303030303030101040406010104d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c997e7209067e7202067e7203067e730b067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7202069d9c720a7e7313067e7203069591720b7314d801d60cb27207731500ed938c720c017316927e8c720c0206720b7317909c7e8cb2720573180002067e7204069c9a720a73199a9c7ec17201067e731a067e72040690b0ada5d9010b639593c2720b731bc1720b731c731dd9010b599a8c720b018c720b02731e731f
      //
      // ErgoTreeTemplate: d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c997e7209067e7202067e7203067e730b067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7202069d9c720a7e7313067e7203069591720b7314d801d60cb27207731500ed938c720c017316927e8c720c0206720b7317909c7e8cb2720573180002067e7204069c9a720a73199a9c7ec17201067e731a067e72040690b0ada5d9010b639593c2720b731bc1720b731c731dd9010b599a8c720b018c720b02731e731f

      // Context (declarations here are only for simulations):

      val MaxExFee: Long                = getConstant(1).get
      val ExFeePerTokenDenom: Long      = getConstant(2).get
      val BaseAmount: Long              = getConstant(3).get
      val FeeNum: Int                   = getConstant(4).get
      val RefundProp: Boolean           = getConstant(5).get
      val SpectrumIsQuote: Boolean      = getConstant(10).get
      val PoolNFT: Coll[Byte]           = getConstant(13).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(14).get
      val QuoteId: Coll[Byte]           = getConstant(15).get
      val MinQuoteAmount: Long          = getConstant(16).get
      val ExFeePerTokenNum: Long        = getConstant(19).get
      val SpectrumId: Coll[Byte]        = getConstant(22).get
      val FeeDenom: Int                 = getConstant(26).get
      val MinerPropBytes: Coll[Byte]    = getConstant(27).get
      val MaxMinerFee: Long             = getConstant(30).get

      // Contract
      val baseAmount         = BaseAmount
      val feeNum             = FeeNum
      val feeDenom           = FeeDenom
      val maxExFee           = MaxExFee
      val exFeePerTokenDenom = ExFeePerTokenDenom
      val exFeePerTokenNum   = ExFeePerTokenNum
      val minQuoteAmount     = MinQuoteAmount

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

        val quoteAsset = rewardBox.tokens(0)
        val quoteAmount =
          if (SpectrumIsQuote) {
            val deltaQuote = quoteAsset._2.toBigInt - maxExFee
            deltaQuote * exFeePerTokenDenom / (exFeePerTokenDenom - exFeePerTokenNum)
          } else {
            quoteAsset._2.toBigInt
          }
        // 1.1.
        val fairExFee =
          if (SpectrumIsQuote) true
          else {
            val exFee     = quoteAmount * exFeePerTokenNum / exFeePerTokenDenom
            val remainder = maxExFee - exFee
            if (remainder > 0) {
              val spectrumRem = rewardBox.tokens(1)
              spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
            } else {
              true
            }
          }

        val relaxedOutput = quoteAmount + 1L // handle rounding loss

        val base_x_feeNum = baseAmount.toBigInt * feeNum
        // 1.2.
        val fairPrice = poolReservesY * base_x_feeNum <= relaxedOutput * (poolReservesX * feeDenom + base_x_feeNum)
        // 1.3.
        val validMinerFee = OUTPUTS.map { (o: Box) =>
          if (o.propositionBytes == MinerPropBytes) o.value else 0L
        }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

        validPoolIn &&
          rewardBox.propositionBytes == RedeemerPropBytes &&
          quoteAsset._1 == QuoteId &&
          quoteAmount >= minQuoteAmount &&
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
