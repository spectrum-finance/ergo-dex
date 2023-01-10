package io.ergodex.core.cfmm3.n2t

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
      // Description: Contract that validates user's deposit into the CFMM n2t Pool.
      //
      // Constants:
      //
      // {1}  -> SelfXAmount[Long]
      // {2}  -> RefundProp[ProveDlog]
      // {10} -> SpectrumIsY[Boolean]
      // {11} -> ExFee[Long]
      // {14} -> PoolNFT[Coll[Byte]]
      // {15} -> RedeemerPropBytes[Coll[Byte]]
      // {20} -> MinerPropBytes[Coll[Byte]]
      // {23} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19d60419040005c0b80208cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040404060402040205feffffffffffffffff0104000404010105d00f040004000e2002020202020202020202020202020202020202020202020202020202020202020e2001010101010101010101010101010101010101010101010101010101010101010404040205c0b80201000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80cd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d6098cb2db6308a773080002d60ab27203730900d60b7e8c720a0206d60c9d9c7e95730a997209730b7209067206720bd60ddb63087204d60eb2720d730c00ededededed938cb27203730d0001730e93c27204730f95ed8f7208720c93b1720d7310d801d60fb2720d731100eded92c1720499c1a77312938c720f018c720a01927e8c720f02069d9c99720c7208720b720695927208720c927ec1720406997ec1a706997e7202069d9c997208720c720772067313938c720e018c720501927e8c720e0206a17208720c90b0ada5d9010f639593c2720f7314c1720f73157316d9010f599a8c720f018c720f0273177318
      //
      // ErgoTreeTemplate: d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80cd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d6098cb2db6308a773080002d60ab27203730900d60b7e8c720a0206d60c9d9c7e95730a997209730b7209067206720bd60ddb63087204d60eb2720d730c00ededededed938cb27203730d0001730e93c27204730f95ed8f7208720c93b1720d7310d801d60fb2720d731100eded92c1720499c1a77312938c720f018c720a01927e8c720f02069d9c99720c7208720b720695927208720c927ec1720406997ec1a706997e7202069d9c997208720c720772067313938c720e018c720501927e8c720e0206a17208720c90b0ada5d9010f639593c2720f7314c1720f73157316d9010f599a8c720f018c720f0273177318

      // Context (declarations here are only for simulations):

      val SelfXAmount: Long             = getConstant(1).get
      val RefundProp: Boolean           = getConstant(2).get
      val SpectrumIsY: Boolean          = getConstant(10).get
      val ExFee: Long                   = getConstant(11).get
      val PoolNFT: Coll[Byte]           = getConstant(14).get
      val RedeemerPropBytes: Coll[Byte] = getConstant(15).get
      val MinerPropBytes: Coll[Byte]    = getConstant(20).get
      val MaxMinerFee: Long             = getConstant(23).get

      // Contract
      val InitiallyLockedLP = 0x7fffffffffffffffL

      val poolIn = INPUTS(0)

      // Validations
      // 1.
      val validDeposit =
        if (INPUTS.size >= 2 && poolIn.tokens.size == 3) {
          val selfY = SELF.tokens(0)

          val selfXAmount = SelfXAmount
          val selfYAmount = if (SpectrumIsY) selfY._2 - ExFee else selfY._2
          // 1.1.
          val validPoolIn = poolIn.tokens(0)._1 == PoolNFT

          val poolLP          = poolIn.tokens(1)
          val reservesXAmount = poolIn.value
          val reservesY       = poolIn.tokens(2)
          val reservesYAmount = reservesY._2

          val supplyLP = InitiallyLockedLP - poolLP._2

          val minByX = selfXAmount.toBigInt * supplyLP / reservesXAmount
          val minByY = selfYAmount.toBigInt * supplyLP / reservesYAmount

          val minimalReward = min(minByX, minByY)

          val rewardOut   = OUTPUTS(1)
          val rewardLP    = rewardOut.tokens(0)

          // 1.2.
          val validChange =
            if (minByX < minByY && rewardOut.tokens.size == 2) {
              val diff    = minByY - minByX
              val excessY = diff * reservesYAmount / supplyLP
              val changeY = rewardOut.tokens(1)

              rewardOut.value >= SELF.value - selfXAmount &&
              changeY._1 == reservesY._1 &&
              changeY._2 >= excessY

            } else if (minByX >= minByY) {
              val diff    = minByX - minByY
              val excessX = diff * reservesXAmount / supplyLP

              rewardOut.value >= SELF.value - (selfXAmount - excessX)
            } else {
              false
            }
          // 1.3.
          val validMinerFee = OUTPUTS.map { (o: Box) =>
            if (o.propositionBytes == MinerPropBytes) o.value else 0L
          }.fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn &&
          rewardOut.propositionBytes == RedeemerPropBytes &&
          validChange &&
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
