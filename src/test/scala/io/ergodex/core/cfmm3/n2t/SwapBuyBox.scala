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
      // {1}  -> RefundProp[ProveDlog]
      // {5}  -> MaxExFee[Long]
      // {6}  -> ExFeePerTokenDenom[Long]
      // {7}  -> ExFeePerTokenNum[Long]
      // {8}  -> BaseAmount[Long]
      // {9}  -> FeeNum[Int]
      // {11} -> PoolNFT[Coll[Byte]]
      // {12} -> RedeemerPropBytes[Coll[Byte]]
      // {13} -> MinQuoteAmount[Long]
      // {16} -> SpectrumId[Coll[Byte]]
      // {20} -> FeeDenom[Int]
      // {21} -> MinerPropBytes[Coll[Byte]]
      // {24} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19fb031a040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040205f015052c05c801060204b0060203e404000e2002020202020202020202020202020202020202020202020202020202020202020e20010101010101010101010101010101010101010101010101010101010101010105c00c050004000e200303030303030303030303030303030303030303030303030303030303030303010105020404060203e80e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d805d602db63087201d603b2a5730400d60499c17203c1a7d6059973059d9c720473067307d6069c73087309ededededed938cb27202730a0001730b93c27203730c927204730d95917205730ed801d607b2db63087203730f00ed938c7207017310928c72070272057311909c7ec172010672069c7e9a72047312069a9c7e8cb2720273130002067314720690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319
      //
      // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d805d602db63087201d603b2a5730400d60499c17203c1a7d6059973059d9c720473067307d6069c73087309ededededed938cb27202730a0001730b93c27203730c927204730d95917205730ed801d607b2db63087203730f00ed938c7207017310928c72070272057311909c7ec172010672069c7e9a72047312069a9c7e8cb2720273130002067314720690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319

      // Context (declarations here are only for simulations):
      val RefundProp: Boolean           = getConstant(1).get
      val MaxExFee: Long                = getConstant(5).get
      val ExFeePerTokenDenom: Long      = getConstant(6).get
      val ExFeePerTokenNum: Long        = getConstant(7).get
      val BaseAmount: Long              = getConstant(8).get
      val FeeNum: Int                   = getConstant(9).get
      val PoolNFT: Coll[Byte]           = getConstant(11).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(12).get
      val MinQuoteAmount: Long          = getConstant(13).get
      val SpectrumId: Coll[Byte]        = getConstant(16).get
      val FeeDenom: Int                 = getConstant(20).get
      val MinerPropBytes: Coll[Byte]    = getConstant(21).get
      val MaxMinerFee: Long             = getConstant(24).get

      // Contract

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

          val quoteAmount   = rewardBox.value - SELF.value
          // 1.1.
          val fairExFee = {
            val exFee     = quoteAmount * ExFeePerTokenNum / ExFeePerTokenDenom
            val remainder = MaxExFee - exFee
            if (remainder > 0) {
              val spectrumRem = rewardBox.tokens(0)
              spectrumRem._1 == SpectrumId && spectrumRem._2 >= remainder
            } else {
              true
            }
          }
          val relaxedOutput = quoteAmount + 1 // handle rounding loss
          val base_x_feeNum = BaseAmount.toBigInt * FeeNum
          // 1.2.
          val fairPrice     = poolReservesX * base_x_feeNum <= relaxedOutput * (poolReservesY * FeeDenom + base_x_feeNum)

          // 1.3.
          val validMinerFee = OUTPUTS.map { (o: Box) =>
            if (o.propositionBytes == MinerPropBytes) o.value else 0L
          }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn &&
          rewardBox.propositionBytes == RedeemerPropBytes &&
          quoteAmount >= MinQuoteAmount &&
          fairExFee &&
          fairPrice &&
          validMinerFee

        } else false

      sigmaProp(RefundProp || validTrade)
    }
}

object SwapBuyBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): SwapBuyBox[F]      =
    new SwapBuyBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[SwapBuyBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
