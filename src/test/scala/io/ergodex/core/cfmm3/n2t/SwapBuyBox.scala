package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{AnyBox, BoxSim, RuntimeState, TryFromBox}

final class SwapBuyBox[F[_]: RuntimeState](
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
      // Name: SwapBuy
      // Description: Contract that validates user's swap from token to ERG in the CFMM n2t Pool.
      //
      // Constants:
      //
      // {1}  -> BaseAmount[Long]
      // {2}  -> FeeNum[Int]
      // {3}  -> RefundProp[ProveDlog]
      // {7}  -> MaxExFee[Long]
      // {8}  -> ExFeePerTokenDenom[Long]
      // {9}  -> ExFeePerTokenNum[Long]
      // {11} -> PoolNFT[Coll[Byte]]
      // {12} -> RedeemerPropBytes[Coll[Byte]]
      // {13} -> MinQuoteAmount[Long]
      // {16} -> SpectrumId[Coll[Byte]]
      // {20} -> FeeDenom[Int]
      // {21} -> MinerPropBytes[Coll[Byte]]
      // {24} -> MaxMinerFee[Long]
      //
      // ErgoTree: 198b041a040005e01204c80f08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040205f015052c05c80104000e2002020202020202020202020202020202020202020202020202020202020202020e20010101010101010101010101010101010101010101010101010101010101010105c00c06010004000e20030303030303030303030303030303030303030303030303030303030303030301010502040404d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d606997e7307069d9c7e7205067e7308067e730906ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310927e8c7207020672067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319
      //
      // ErgoTreeTemplate: d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d606997e7307069d9c7e7205067e7308067e730906ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310927e8c7207020672067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319

      // Context (declarations here are only for simulations):
      val BaseAmount: Long              = getConstant(1).get
      val FeeNum: Int                   = getConstant(2).get
      val RefundProp: Boolean           = getConstant(3).get
      val MaxExFee: Long                = getConstant(7).get
      val ExFeePerTokenNum: Long        = getConstant(9).get
      val ExFeePerTokenDenom: Long      = getConstant(8).get
      val PoolNFT: Coll[Byte]           = getConstant(11).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(12).get
      val MinQuoteAmount: Long          = getConstant(13).get
      val SpectrumId: Coll[Byte]        = getConstant(16).get
      val FeeDenom: Int                 = getConstant(20).get
      val MinerPropBytes: Coll[Byte]    = getConstant(21).get
      val MaxMinerFee: Long             = getConstant(24).get

      // Contract
      val baseAmount         = BaseAmount
      val feeNum             = FeeNum
      val feeDenom           = FeeDenom
      val maxExFee           = MaxExFee
      val exFeePerTokenDenom = ExFeePerTokenDenom
      val exFeePerTokenNum   = ExFeePerTokenNum
      val minQuoteAmount     = MinQuoteAmount

      val poolIn = INPUTS(0)

      // Validations
      // 1.
      val validTrade =
      if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
        val poolNFT = poolIn.tokens(0)._1 // first token id is NFT

        val poolReservesX = poolIn.value.toBigInt // nanoErgs is X asset amount
        val poolReservesY = poolIn.tokens(2)._2.toBigInt // third token amount is Y asset amount

        val validPoolIn = poolNFT == PoolNFT

        val rewardBox = OUTPUTS(1)

        val quoteAmount = rewardBox.value - SELF.value
        // 1.1.
        val fairExFee = {
          val exFee     = quoteAmount.toBigInt * exFeePerTokenNum / exFeePerTokenDenom
          val remainder = maxExFee - exFee
          if (remainder > 0) {
            val spectrumRem = rewardBox.tokens(0)
            spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
          } else {
            true
          }
        }
        val relaxedOutput = quoteAmount + 1L // handle rounding loss
        val base_x_feeNum = baseAmount.toBigInt * feeNum
        // 1.2.
        val fairPrice = poolReservesX * base_x_feeNum <= relaxedOutput * (poolReservesY * feeDenom + base_x_feeNum)
        // 1.3.
        val validMinerFee = OUTPUTS.map { (o: Box) =>
          if (o.propositionBytes == MinerPropBytes) o.value else 0L
        }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

        validPoolIn &&
          rewardBox.propositionBytes == RedeemerPropBytes &&
          quoteAmount >= minQuoteAmount &&
          fairExFee &&
          fairPrice &&
          validMinerFee

      } else false

      sigmaProp(RefundProp || validTrade)
    }
}

object SwapBuyBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): SwapBuyBox[F] =
    new SwapBuyBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[SwapBuyBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
