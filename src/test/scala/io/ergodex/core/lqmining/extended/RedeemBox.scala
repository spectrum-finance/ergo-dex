package io.ergodex.core.lqmining.extended

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.{AnyBox, BoxSim, RuntimeState, TryFromBox}
import io.ergodex.core.syntax._

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
      // Context (declarations here are only for simulations):
      val RefundPk: Boolean              = getConstant(0).get
      val RedeemerProp: Coll[Byte]       = getConstant(5).get
      val ExpectedLQ: Coll[Byte]         = getConstant(6).get
      val ExpectedLQAmount: Long         = getConstant(7).get
      val MinerPropBytes: Coll[Byte]     = getConstant(9).get
      val MaxMinerFee: Long              = getConstant(12).get

      // ===== Contract Information ===== //
      // Name: Redeem (extended LM)
      // Description: Contract that validates user's redeem from the extended LM Pool.
      //
      // Tokens:
      //   0:
      //     _1: BundleKeyId
      //     _2: 0x7fffffffffffffffL - 1L
      //
      // Constants:
      //  {0} -> RefundPk[ProveDlog]
      //  {5} -> RedeemerProp[Coll[Byte]]
      //  {6} -> ExpectedLQ[Coll[Byte]]
      //  {7} -> ExpectedLQAmount[Long]
      //  {9} -> MinerPropBytes[Coll[Byte]]
      //  {12} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19ca020e08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040400040c04020e2001010101010101010101010101010101010101010101010101010101010101010e20000000000000000000000000000000000000000000000000000000000000000005d00f04000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100eb027300d195ed92b1a4730190b1db6308b2a47302007303d801d601b2a5730400eded93c27201730593860273067307b2db6308720173080090b0ada5d90102639593c272027309c17202730a730bd90102599a8c7202018c720202730c730d
      //
      // ErgoTreeTemplate: eb027300d195ed92b1a4730190b1db6308b2a47302007303d801d601b2a5730400eded93c27201730593860273067307b2db6308720173080090b0ada5d90102639593c272027309c17202730a730bd90102599a8c7202018c720202730c730d
      //
      // ErgoTreeTemplateHash: 109121331e81add98157a33d5cf712403075570cef8171b9e4ed15340689054c
      //

      // ===== Getting INPUTS data ===== //
      val poolIn = INPUTS(0)

      val validRedeem =
        if (INPUTS.size >= 2 && poolIn.tokens.size <= 6) {
          // ===== Getting OUTPUTS data ===== //
          val redeemerOut = OUTPUTS(1)

          // ===== Validating conditions ===== //
          // 1.
          val validRedeemerOut = {
            (redeemerOut.propositionBytes == RedeemerProp) &&
              ((ExpectedLQ, ExpectedLQAmount) == redeemerOut.tokens(0))
          }
          // 2.
          val validMinerFee = OUTPUTS
            .map { (o: Box) =>
              if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }
            .fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validRedeemerOut && validMinerFee

        } else false

      sigmaProp(RefundPk || validRedeem)
    }
}
object RedeemBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): RedeemBox[F] =
    new RedeemBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[RedeemBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
