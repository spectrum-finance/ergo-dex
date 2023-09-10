package io.ergodex.core.lqmining.extended

import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{BoxSim, RuntimeState}

final class DepositBudgetBox[F[_]: RuntimeState](
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
      val RefundPk: Boolean          = getConstant(1).get
      val PoolId: Coll[Byte]         = getConstant(5).get
      val RedeemerProp: Coll[Byte]   = getConstant(10).get
      val MinerPropBytes: Coll[Byte] = getConstant(12).get
      val MaxMinerFee: Long          = getConstant(15).get

      // Name: DepositBudget
      // Description: Contract that validates budget deposit into the extended LM Pool.
      //
      // ===== Deposit Box ===== //
      // Tokens:
      //   0:
      //     _1: Reward Token ID
      //     _2: Amount of Reward Tokens to deposit
      //
      // Constants:
      // {1}  -> RefundPk[ProveDlog]
      // {5}  -> PoolId[Coll[Byte]]
      // {10}  -> RedeemerProp[Coll[Byte]]
      // {12} -> MinerPropBytes[Coll[Byte]]
      // {15} -> MaxMinerFee[Long]
      //
      // ErgoTree: 19a60311040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040c04000e20020202020202020202020202020202020202020202020202020202020202020204000402040a04000e20000000000000000000000000000000000000000000000000000000000000000004000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730290b1db630872017303d806d602db63087201d603938cb27202730400017305d604e4e30004d60595937204730673077308d606b27202720500d607b2db6308a7730900eded7203ededed720393730ad0b2e4c672010614720400938c7206018c72070193998cb2db6308b2a5730b00720500028c7206028c72070290b0ada5d90108639593c27208730cc17208730d730ed90108599a8c7208018c720802730f7310
      //
      // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730290b1db630872017303d806d602db63087201d603938cb27202730400017305d604e4e30004d60595937204730673077308d606b27202720500d607b2db6308a7730900eded7203ededed720393730ad0b2e4c672010614720400938c7206018c72070193998cb2db6308b2a5730b00720500028c7206028c72070290b0ada5d90108639593c27208730cc17208730d730ed90108599a8c7208018c720802730f7310
      //
      // ErgoTreeTemplateHash: 3c9b030883cb4fdf9a03dbe8e63d33cbfb26e73dccec33a9c99ff7e67f73b1be
      //

      // ===== Getting INPUTS data ===== //
      val poolIn = INPUTS(0)

      val validDeposit =
        if (INPUTS.size >= 2 && poolIn.tokens.size <= 6) {
          // ===== Getting SELF data ===== //
          val deposit = SELF.tokens(0)

          // ===== Getting OUTPUTS data ===== //
          val poolOut = OUTPUTS(0) // 0 -> pool_out, 1 -> redeemer_out,

          // ===== Validating conditions ===== //
          // 3.
          val validPoolIn = poolIn.tokens(0)._1 == PoolId

          val validPoolOut = {
            val budgetRedeemers = poolIn.R6[Coll[SigmaProp]].get
            val BudgetInd: Int  = getVar[Int](0).get
            val budgetTokenId   = if (BudgetInd == 0) 1 else 5
            val budgetDelta     = poolOut.tokens(budgetTokenId)._2 - poolIn.tokens(budgetTokenId)._2
            (poolIn.tokens(0)._1 == PoolId) &&
              (RedeemerProp == budgetRedeemers(BudgetInd).propBytes) &&
              (poolIn.tokens(budgetTokenId)._1 == deposit._1) &&
              (budgetDelta == deposit._2)

          }
          // 4.
          val validMinerFee = OUTPUTS
            .map { (o: Box) =>
              if (o.propositionBytes == MinerPropBytes) o.value else 0L
            }
            .fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

          validPoolIn && validPoolOut && validMinerFee

        } else false

      sigmaProp(RefundPk || validDeposit)
    }
}
