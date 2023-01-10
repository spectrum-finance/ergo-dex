package io.ergodex.core.cfmm3.t2t

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{AnyBox, BoxSim, RuntimeState, TryFromBox}

final class DepositBox[F[_]: RuntimeState](
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
      // Name: Deposit
      // Description: Contract that validates user's deposit into the CFMM t2t Pool.
      //
      // Constants:
      //
      // {1} -> RefundProp[ProveDlog]
      // {8} -> SelfXAmount[Long] // SELF.tokens(0)._2 - ExFee
      // {10} -> SelfYAmount[Long] // SELF.tokens(1)._2 - ExFee
      // {13} -> PoolNFT[Coll[Byte]]
      // {14} -> RedeemerPropBytes[Coll[Byte]]
      // {21} -> MinerPropBytes[Coll[Byte]]
      // {24} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19db041a040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040404080402040205feffffffffffffffff010404060207d0040606020bb8040004000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010404040204040402010101000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c730872057207d609b27202730900d60a7e8c72090206d60b9d9c730a7205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319
      //
      // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c730872057207d609b27202730900d60a7e8c72090206d60b9d9c730a7205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319

      // Context (declarations here are only for simulations):
      val RefundProp: Boolean           = getConstant(1).get
      val SelfXAmount: Long             = getConstant(8).get
      val SelfYAmount: Long             = getConstant(10).get
      val PoolNFT: Coll[Byte]           = getConstant(13).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(14).get
      val MinerPropBytes: Coll[Byte]    = getConstant(21).get
      val MaxMinerFee: Long             = getConstant(24).get

      // Contract
      val InitiallyLockedLP = 0x7fffffffffffffffL

      val poolIn = INPUTS(0)

      // Validations
      // 1.
      val validDeposit =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 4) {
          val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

          val poolLP    = poolIn.tokens(1)
          val reservesX = poolIn.tokens(2)
          val reservesY = poolIn.tokens(3)

          val reservesXAmount = reservesX._2
          val reservesYAmount = reservesY._2

          val supplyLP = InitiallyLockedLP - poolLP._2

          val minByX = SelfXAmount.toBigInt * supplyLP / reservesXAmount
          val minByY = SelfYAmount.toBigInt * supplyLP / reservesYAmount

          val minimalReward = min(minByX, minByY)

          val rewardOut        = OUTPUTS(1)
          val rewardLP         = rewardOut.tokens(0)
          // 1.1.
          val validErgChange   = rewardOut.value >= SELF.value
          // 1.2.
          val validTokenChange =
            if (minByX < minByY && rewardOut.tokens.size == 2) {
              val diff    = minByY - minByX
              val excessY = diff * reservesYAmount / supplyLP

              val changeY = rewardOut.tokens(1)

              changeY._1 == reservesY._1 &&
              changeY._2 >= excessY
            } else if (minByX > minByY && rewardOut.tokens.size == 2) {
              val diff    = minByX - minByY
              val excessX = diff * reservesXAmount / supplyLP

              val changeX = rewardOut.tokens(1)

              changeX._1 == reservesX._1 &&
              changeX._2 >= excessX
            } else if (minByX == minByY) {
              true
            } else {
              false
            }

          // 1.3.
          val validMinerFee = OUTPUTS.map { (o: Box) =>
            if (o.propositionBytes == MinerPropBytes) o.value else 0L
          }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn &&
          rewardOut.propositionBytes == RedeemerPropBytes &&
          validErgChange &&
          validTokenChange &&
          rewardLP._1 == poolLP._1 &&
          rewardLP._2 >= minimalReward &&
          validMinerFee

        } else false

      sigmaProp(RefundProp || validDeposit)
    }
}

object DepositBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): DepositBox[F]      =
    new DepositBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[DepositBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
