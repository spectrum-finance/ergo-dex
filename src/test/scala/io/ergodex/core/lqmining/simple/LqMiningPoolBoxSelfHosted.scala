package io.ergodex.core.lqmining.simple

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{AnyBox, BoxSim, RuntimeState, TryFromBox}

final class LqMiningPoolBoxSelfHosted[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val validatorBytes: String
) extends BoxSim[F] {

  val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      val BundleScriptHash: Coll[Byte] = getConstant(23).get
      // ===== Contract Information ===== //
      // Name: LMPoolSelfHosted
      // Description: Contract that validates a change in the LM pool's state.
      //
      // ===== LM Pool Box ===== //
      // Registers:
      //   R4[Coll[Int]]: LM program config
      //      0: Length of every epoch in blocks
      //      1: Number of epochs in the LM program
      //      2: Program start
      //      3: Redeem blocks delta  // the number of blocks after the end of LM program, at which redeems can be performed without any restrictions.
      //   R5[Long]: Program budget  // total budget of LM program.
      //   R6[Long]: MaxRoundingError // Tokens rounding delta max value.
      //   R7[Int]: Epoch index  // index of the epoch being compounded (required only for compounding).
      //
      // Tokens:
      //   0:
      //     _1: LM Pool NFT
      //     _2: Amount: 1
      //   1:
      //     _1: Reward Token ID
      //     _2: Amount: <= Program budget.
      //   2:
      //     _1: LQ Token ID  // locked LQ tokens.
      //     _2: Amount of LQ tokens.
      //   3:
      //     _1: vLQ Token ID  // tokens representing locked share of LQ.
      //     _2: Amount of vLQ tokens.
      //   4:
      //     _1: TMP Token ID  // left program epochs times liquidity.
      //     _2: Amount of TMP tokens.
      //
      // ErgoTree: 19b5052204000402040204040404040604060408040804040402040004000402040204000400040a050004040402040205000e20599e30a83bc971f75582f2581f0633eebfe936b95d956ed103cbec520d8043860400050004020402040605000500040205000500d81cd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e4c6a70605d607b27202730100d608b27203730200d609b27202730300d60ab27203730400d60bb27202730500d60c8c720b01d60db27203730600d60eb27202730700d60fb27203730800d6108c720a02d611998c7209027210d6128c720802d613b27204730900d6149a99a37213730ad615b27204730b00d6169d72147215d61795919e72147215730c9a7216730d7216d618b27204730e00d6197e721805d61a9d72057219d61b998c720b028c720d02d61c998c720e028c720f02d1ededededed93b27202730f00b27203731000ededed93e4c672010410720493e4c672010505720593e4c6720106057206928cc77201018cc7a70193c27201c2a7ededed938c7207018c720801938c7209018c720a0193720c8c720d01938c720e018c720f0193b172027311959172117312d801d61db2a5731300ededed929a997205721272069c7e9995907217721872179a72187314731505721a937211f0721b939c7211997219a273167e721705f0721ced93cbc2721d731793b2db6308721d7318008602720c7211958f72117319ededec929a997205721272069c7e9995907217721872179a7218731a731b05721a92a39a9a72139c72187215b27204731c0093721bf0721192721c959172177218731d9c721b997219a2731e7e721705d801d61de4c672010704edededed90721d997217731f909972129c7e997218721d05721a9a721a7206907ef0998c7207027212069d9c7e721a067e721c067e721006937211732093721b7321
      //
      // ErgoTreeTemplate: d81cd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e4c6a70605d607b27202730100d608b27203730200d609b27202730300d60ab27203730400d60bb27202730500d60c8c720b01d60db27203730600d60eb27202730700d60fb27203730800d6108c720a02d611998c7209027210d6128c720802d613b27204730900d6149a99a37213730ad615b27204730b00d6169d72147215d61795919e72147215730c9a7216730d7216d618b27204730e00d6197e721805d61a9d72057219d61b998c720b028c720d02d61c998c720e028c720f02d1ededededed93b27202730f00b27203731000ededed93e4c672010410720493e4c672010505720593e4c6720106057206928cc77201018cc7a70193c27201c2a7ededed938c7207018c720801938c7209018c720a0193720c8c720d01938c720e018c720f0193b172027311959172117312d801d61db2a5731300ededed929a997205721272069c7e9995907217721872179a72187314731505721a937211f0721b939c7211997219a273167e721705f0721ced93cbc2721d731793b2db6308721d7318008602720c7211958f72117319ededec929a997205721272069c7e9995907217721872179a7218731a731b05721a92a39a9a72139c72187215b27204731c0093721bf0721192721c959172177218731d9c721b997219a2731e7e721705d801d61de4c672010704edededed90721d997217731f909972129c7e997218721d05721a9a721a7206907ef0998c7207027212069d9c7e721a067e721c067e721006937211732093721b7321
      //
      // Validations:
      // 1. LM Pool NFT is preserved;
      // 2. LM Pool Config, LM program budget, maxRoundingError and creationHeight are preserved;
      // 3. LMPool validation script is preserved;
      // 4. LM Pool assets are preserved;
      // 5. There are no illegal tokens in LM Pool;
      // 6. Action is valid:
      //    6.1. Deposit: if (deltaLQ > 0)
      //         6.1.1. Previous epochs are compounded;
      //         6.1.2. Delta LQ tokens amount is correct;
      //         6.1.3. Delta TMP tokens amount is correct.
      //    6.2. Redeem: elif if (deltaLQ < 0)
      //         6.2.1. Previous epochs are compounded;
      //         6.2.2. Delta LQ tokens amount is correct;
      //         6.2.3. Delta TMP tokens amount is correct.
      //    6.3. Compound: else
      //         6.3.1. Epoch is legal to perform compounding;
      //         6.3.2. Previous epoch is compounded;
      //         6.3.3. Delta reward tokens amount equals to calculated reward amount;
      //         6.3.4. Delta LQ tokens amount is 0;
      //         6.3.5. Delta vLQ tokens amount is 0.
      //
      // ===== Getting SELF data ===== //
      val poolNFT0 = SELF.tokens(0)
      val poolX0   = SELF.tokens(1)
      val poolLQ0  = SELF.tokens(2)
      val poolVLQ0 = SELF.tokens(3)
      val poolTMP0 = SELF.tokens(4)

      val conf0            = SELF.R4[Coll[Int]].get
      val epochLen         = conf0(0)
      val epochNum         = conf0(1)
      val programStart     = conf0(2)
      val redeemLimitDelta = conf0(3)

      val creationHeight0 = SELF.creationInfo._1

      val programBudget0    = SELF.R5[Long].get
      val maxRoundingError0 = SELF.R6[Long].get

      // ===== Getting OUTPUTS data ===== //
      val successor = OUTPUTS(0)

      val poolNFT1 = successor.tokens(0)
      val poolX1   = successor.tokens(1)
      val poolLQ1  = successor.tokens(2)
      val poolVLQ1 = successor.tokens(3)
      val poolTMP1 = successor.tokens(4)

      val creationHeight1 = successor.creationInfo._1
      val conf1           = successor.R4[Coll[Int]].get

      val programBudget1    = successor.R5[Long].get
      val maxRoundingError1 = successor.R6[Long].get

      // ===== Getting deltas ===== //
      val reservesX  = poolX0._2
      val reservesLQ = poolLQ0._2

      val deltaX   = poolX1._2 - reservesX
      val deltaLQ  = poolLQ1._2 - reservesLQ
      val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
      val deltaTMP = poolTMP1._2 - poolTMP0._2

      // ===== Calculating epoch parameters ===== //
      val epochAlloc    = programBudget0 / epochNum
      val curBlockIx    = HEIGHT - programStart + 1
      val curEpochIxRem = curBlockIx % epochLen
      val curEpochIxR   = curBlockIx / epochLen
      val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

      // ===== Validating conditions ===== //
      // 1.
      val nftPreserved = poolNFT1 == poolNFT0
      // 2.
      val configPreserved =
        (conf1 == conf0) &&
        (programBudget1 == programBudget0) &&
        (maxRoundingError1 == maxRoundingError0) &&
        (creationHeight1 >= creationHeight0)

      // 3.
      val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
      // 4.
      val assetsPreserved =
        poolX1._1 == poolX0._1 &&
        poolLQ1._1 == poolLQ0._1 &&
        poolVLQ1._1 == poolVLQ0._1 &&
        poolTMP1._1 == poolTMP0._1
      // 5.
      val noMoreTokens = successor.tokens.size == 5
      // 6.
      val validAction = {
        if (deltaLQ > 0) { // deposit
          // 6.1.
          val releasedVLQ     = deltaLQ
          val epochsAllocated = epochNum - max(0L, curEpochIx)
          val releasedTMP     = releasedVLQ * epochsAllocated
          // 6.1.1.
          val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
          val prevEpochsCompoundedForDeposit =
            ((programBudget0 - reservesX) + maxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc

          val bundleOut = OUTPUTS(2)
          val validBundle =
            blake2b256(bundleOut.propositionBytes) == BundleScriptHash &&
            (poolVLQ0._1, releasedVLQ) == bundleOut.tokens(0) &&
            (poolTMP0._1, releasedTMP) == bundleOut.tokens(1) &&
            bundleOut.R4[SigmaProp].isDefined &&
            bundleOut.R5[Coll[Byte]].get == poolNFT0._1

          prevEpochsCompoundedForDeposit &&
          // 6.1.2. && 6.1.3.
          deltaLQ == -deltaVLQ &&
          releasedTMP == -deltaTMP &&
          validBundle

        } else if (deltaLQ < 0) { // redeem
          // 6.2.
          val releasedLQ = deltaVLQ
          val minReturnedTMP =
            if (curEpochIx > epochNum) 0L
            else {
              val epochsDeallocated = epochNum - max(0L, curEpochIx)
              releasedLQ * epochsDeallocated
            }
          // 6.2.1.
          val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
          val prevEpochsCompoundedForRedeem =
            ((programBudget0 - reservesX) + maxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc
          val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

          (prevEpochsCompoundedForRedeem || redeemNoLimit) &&
          // 6.2.2. & 6.2.3.
          (deltaVLQ == -deltaLQ) &&
          (deltaTMP >= minReturnedTMP)

        } else { // compound
          // 6.3.
          val epoch               = successor.R7[Int].get
          val epochsToCompound    = epochNum - epoch
          val prevEpochCompounded = (reservesX - epochsToCompound * epochAlloc) <= (epochAlloc + maxRoundingError0)

          val legalEpoch = epoch <= curEpochIx - 1
          val reward     = epochAlloc.toBigInt * deltaTMP / reservesLQ

          // 6.3.1. && 6.3.2. && 6.3.3. && 6.3.4. && 6.3.5.
          legalEpoch &&
          prevEpochCompounded &&
          (-deltaX <= reward) &&
          (deltaLQ == 0L) &&
          (deltaVLQ == 0L)
        }
      }
      sigmaProp(
        nftPreserved &&
        configPreserved &&
        scriptPreserved &&
        assetsPreserved &&
        noMoreTokens &&
        validAction
      )
    }
}

object LqMiningPoolBoxSelfHosted {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): LqMiningPoolBoxSelfHosted[F] =
    new LqMiningPoolBoxSelfHosted(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[LqMiningPoolBoxSelfHosted, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
