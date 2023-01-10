package io.ergodex.core.cfmm3.n2t

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{AnyBox, BoxSim, RuntimeState, TryFromBox}

final class RedeemBox[F[_]: RuntimeState](
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
      // Name: Redeem
      // Description: Contract that validates user's redeem from the CFMM n2t Pool.
      //
      // Constants:
      //
      // {1}  -> RefundProp[ProveDlog]
      // {11} -> PoolNFT[Coll[Byte]]
      // {12} -> RedeemerPropBytes[Coll[Byte]]
      // {13} -> MinerPropBytes[Coll[Byte]]
      // {16} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19c50312040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040406040204000404040005feffffffffffffffff01040204000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d806d602db63087201d603b2a5730400d604b2db63087203730500d605b27202730600d6067e8cb2db6308a77307000206d6077e9973088cb272027309000206ededededed938cb27202730a0001730b93c27203730c938c7204018c720501927e99c17203c1a7069d9c72067ec17201067207927e8c720402069d9c72067e8c72050206720790b0ada5d90108639593c27208730dc17208730e730fd90108599a8c7208018c72080273107311
      //
      // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d806d602db63087201d603b2a5730400d604b2db63087203730500d605b27202730600d6067e8cb2db6308a77307000206d6077e9973088cb272027309000206ededededed938cb27202730a0001730b93c27203730c938c7204018c720501927e99c17203c1a7069d9c72067ec17201067207927e8c720402069d9c72067e8c72050206720790b0ada5d90108639593c27208730dc17208730e730fd90108599a8c7208018c72080273107311

      // Context (declarations here are only for simulations):
      val RefundProp: Boolean           = getConstant(1).get
      val PoolNFT: Coll[Byte]           = getConstant(11).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(12).get
      val MinerPropBytes: Coll[Byte]    = getConstant(13).get
      val MaxMinerFee: Long             = getConstant(16).get

      // Contract
      val InitiallyLockedLP             = 0x7fffffffffffffffL

      val poolIn      = INPUTS(0)

      // Validations
      // 1.
      val validRedeem =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
          val selfLP = SELF.tokens(0)
          // 1.1.
          val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

          val poolLP          = poolIn.tokens(1)
          val reservesXAmount = poolIn.value
          val reservesY       = poolIn.tokens(2)

          val supplyLP = InitiallyLockedLP - poolLP._2

          val minReturnX = selfLP._2.toBigInt * reservesXAmount / supplyLP
          val minReturnY = selfLP._2.toBigInt * reservesY._2 / supplyLP

          val returnOut = OUTPUTS(1)

          val returnXAmount = returnOut.value - SELF.value
          val returnY       = returnOut.tokens(0)
          // 1.2.
          val validMinerFee = OUTPUTS.map { (o: Box) =>
            if (o.propositionBytes == MinerPropBytes) o.value else 0L}
            .fold(0L, { ( a: Long, b: Long) => a + b } ) <= MaxMinerFee

          validPoolIn &&
          returnOut.propositionBytes == RedeemerPropBytes &&
          returnY._1 == reservesY._1 && // token id matches
          returnXAmount >= minReturnX &&
          returnY._2 >= minReturnY &&
          validMinerFee

        } else false

      sigmaProp(RefundProp || validRedeem)
    }
}
object RedeemBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): RedeemBox[F]      =
    new RedeemBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[RedeemBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
