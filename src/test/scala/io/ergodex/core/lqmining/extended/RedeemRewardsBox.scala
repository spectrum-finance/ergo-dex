package io.ergodex.core.lqmining.extended

import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{BoxSim, RuntimeState}

final class RedeemRewardsBox[F[_]: RuntimeState](
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
      val RefundPk: Boolean                = getConstant(0).get
      val RedeemerProp: Coll[Byte]         = getConstant(3).get
      val ExpectedReward: Coll[Byte]       = getConstant(6).get
      val ExpectedRewardAmount: Coll[Byte] = getConstant(7).get
      val MinerPropBytes: Coll[Byte]       = getConstant(9).get
      val MaxMinerFee: Long                = getConstant(12).get

      // ===== Contract Information ===== //
      // Name: RedeemRewards
      // Description: Contract that validates user's redeem from the extended LM Pool.
      //
      // Tokens:
      //   0:
      //     _1: BundleKeyId
      //     _2: 0x7fffffffffffffffL - 1L
      //
      // Constants:
      //  {0} -> RefundPk[ProveDlog]
      //  {3} -> RedeemerProp[Coll[Byte]]
      //  {6} -> ExpectedReward[Coll[Byte]]
      //  {7} -> ExpectedRewardAmount[Long]
      //  {9} -> MinerPropBytes[Coll[Byte]]
      //  {12} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19d0020e08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040404020e200101010101010101010101010101010101010101010101010101010101010101040004000e20000000000000000000000000000000000000000000000000000000000000000005d00f04020e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100eb027300d19593b1a47301d801d601b2a5730200ededed93c27201730393b2db6308a7730400b2db6308720173050093860273067307b2db6308720173080090b0ada5d90102639593c272027309c17202730a730bd90102599a8c7202018c720202730c730d
      //
      // ErgoTreeTemplate: eb027300d19593b1a47301d801d601b2a5730200ededed93c27201730393b2db6308a7730400b2db6308720173050093860273067307b2db6308720173080090b0ada5d90102639593c272027309c17202730a730bd90102599a8c7202018c720202730c730d
      //
      // ErgoTreeTemplateHash: 918da1cd52a10811af9f533c2c02fe13ac062fa54f4757b4f510cbad5dab0758
      //

      // ===== Getting INPUTS data ===== //
      val validRedeem =
        if (INPUTS.size == 2) {
          // ===== Getting OUTPUTS data ===== //
          val redeemerOut = OUTPUTS(1)

          // ===== Validating conditions ===== //
          // 1.
          val validRedeemerOut = {
            (redeemerOut.propositionBytes == RedeemerProp) &&
              (SELF.tokens(0) == redeemerOut.tokens(0)) &&
              ((ExpectedReward, ExpectedRewardAmount) == redeemerOut.tokens(1))

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
