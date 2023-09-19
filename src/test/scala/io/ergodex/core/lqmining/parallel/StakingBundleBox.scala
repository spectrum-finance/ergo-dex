package io.ergodex.core.lqmining.parallel

import io.ergodex.core.BoxRuntime.NonRunnable
import io.ergodex.core.Helpers.hex
import io.ergodex.core.RuntimeState._
import io.ergodex.core._
import io.ergodex.core.syntax._

final class StakingBundleBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val constants: Map[Int, Any],
  override val validatorBytes: String = hex("staking_bundle")
) extends BoxSim[F] {

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      val actionId: Int = getConstant(1).get
      // ===== Contract Information ===== //
      // Name: StakingBundle (parallel LM)
      // Description: Contract that validates a compounding in the parallel LM pool.
      //
      // ===== Bundle Box ===== //
      // Registers:
      //   R4[Coll[Byte]]: BundleKeyToken name.
      //   R5[Coll[Byte]]: BundleKeyToken info.
      //   R6[Coll[Byte]]: LM Pool ID (tokenId).       // used to authenticate pool.
      //
      // Tokens:
      //   0:
      //     _1: vLQ Token ID  // tokens representing locked share of LQ.
      //     _2: Amount of vLQ tokens.
      //   1:
      //     _1: TMP Token ID  // left program epochs times liquidity.
      //     _2: Amount of the TMP tokens.
      //   2:
      //     _1: BundleKeyId // BundleKeyToken.
      //     _2: 1L
      //   3:
      //     _1: Main reward Token ID.
      //     _2: Amount of main reward tokens.
      //   4:
      //     _1: Optional reward Token ID.
      //     _2: Amount of optional reward tokens.
      //
      // Constants:
      //  {1} -> actionId[Int]  // 0 - deposit/compound/redeem;
      //                           1 - redeem rewards.
      //
      // ContextExtension constants:
      // If (actionId == 0):
      //  0: Int - redeemer output index;
      //  1: Int - successor output index.
      // * indexes are dynamic to allow batch compounding.
      // If (actionId == 1):
      //  0: Int - bundleKeyToken index;
      //  1: Int - reward token index.
      //
      // Validations:
      // 1. LM Pool NFT (Token ID) is valid;
      // 2. Action is valid:
      //    - Compound:
      //      -- Valid TMP And bundleKey;
      //      -- Valid successor;
      //      -- Valid reward.
      //    - Redeem:
      //      -- Valid bundleKey tokens provided.
      //    - Redeem rewards:
      //      -- Valid bundleKey tokens provided;
      //      -- Reward is valid;
      //      -- Out Bundle is valid.
      //
      // Limitations:
      // 1. After Redeem rewards at least 1 reward token of any type must stay in the bundle;
      // 2. Redeem/Redeem rewards can be performed only by input with 0x7fffffffffffffffL - 1L
      //    bundleKeyId tokens unique for every Bundle.
      //
      // Compounding Tx:
      //    INPUTS:  (0 -> pool_in,
      //              1 -> bundle_in
      //              ...).
      //    OUTPUTS: (0 -> pool_out,
      //              1 -> bundle_out
      //              ...).
      //
      // ErgoTree: 19a2041904000e2002020202020202020202020202020202020202020202020202020202020202020e20000000000000000000000000000000000000000000000000000000000000000008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040c040204040400040005fcffffffffffffffff0104000e200508f3623d4b2be3bdb9737b3e65644f011167eefb830d9965205f022ceda40d04060400040804140402050204040e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d803d601b2a4730000d6027301d6037302eb027303d195ed92b1a4730490b1db630872017305d805d604db63087201d605b2a5730600d606c57201d607b2a5730700d6088cb2db6308a773080002ededed938cb27204730900017202ed93c2720572039386027206730ab2db63087205730b00ededededed93cbc27207730c93d0e4c672070608720393e4c67207070e72029386028cb27204730d00017208b2db63087207730e009386028cb27204730f00019c72087e731005b2db6308720773110093860272067312b2db6308720773130090b0ada5d90109639593c272097314c1720973157316d90109599a8c7209018c72090273177318
      //
      // ErgoTreeTemplate: d803d601b2a4730000d6027301d6037302eb027303d195ed92b1a4730490b1db630872017305d805d604db63087201d605b2a5730600d606c57201d607b2a5730700d6088cb2db6308a773080002ededed938cb27204730900017202ed93c2720572039386027206730ab2db63087205730b00ededededed93cbc27207730c93d0e4c672070608720393e4c67207070e72029386028cb27204730d00017208b2db63087207730e009386028cb27204730f00019c72087e731005b2db6308720773110093860272067312b2db6308720773130090b0ada5d90109639593c272097314c1720973157316d90109599a8c7209018c72090273177318
      //
      // ErgoTreeTemplateHash: 9125d9488d38c942ab3a6a212c05f9c0d8d7fe6c3cb85c3638459e432a99cfbb
      //
      // ===== Validating conditions ===== //
      val validStateTransition = {
        if (actionId == 0) { // Deposit/Compound/Redeem.
          // ===== Getting INPUTS data ===== //
          val bundleVLQ0 = SELF.tokens(0)
          val poolId0    = SELF.R6[Coll[Byte]].get

          val pool0 = INPUTS(0)

          val poolMainRewardToken0 = pool0.tokens(1)
          val poolMainReward0      = poolMainRewardToken0._2

          val poolReservesLQ0 = pool0.tokens(2)._2

          val poolTMP0         = pool0.tokens(4)
          val poolReservesTMP0 = poolTMP0._2

          val poolOptRewardToken0 = pool0.tokens(5)
          val poolOptReward0      = poolOptRewardToken0._2

          val conf = pool0.R4[Coll[Int]].get

          // ===== Getting OUTPUTS data ===== //
          val pool1   = OUTPUTS(0)
          val deltaLQ = pool1.tokens(2)._2 - poolReservesLQ0

          val epochNum = conf(1)
          val epoch    = pool0.R8[Int].get + 1
          val epochsToCompound =
            if (epoch <= 0) epochNum else if (epoch > 0 && epoch < epochNum) epochNum - epoch else 0

          // Valid Pool:
          val validPool = pool1.tokens(0)._1 == poolId0

          // Valid action:
          val validAction =
            if (deltaLQ == 0L) { // Compound.
              // ===== Getting INPUTS data ===== //
              val bundleTMP0 = SELF.tokens(1)
              val bundleKey0 = SELF.tokens(2)._1

              val budgets0               = pool0.R5[Coll[Long]].get
              val prevMainProgramBudget0 = budgets0(0)
              val prevOptProgramBudget0  = budgets0(1)

              val successorIndex = getVar[Int](1).get

              // ===== Getting OUTPUTS data ===== //
              val successor = OUTPUTS(successorIndex)

              val bundleVLQ1 = successor.tokens(0)

              // ===== Getting deltas and calculate rewards ===== //
              val bundleVLQ   = bundleVLQ0._2
              val bundleTMP   = bundleTMP0._2
              val releasedTMP = bundleTMP0._2 - epochsToCompound * bundleVLQ
              val actualTMP   = 0x7fffffffffffffffL - poolReservesTMP0 - poolReservesLQ0 * epochsToCompound

              val epochNumToEnd = epochsToCompound + 1 // to recalculate epoch allocations.

              val epochMainAlloc = prevMainProgramBudget0 / epochNumToEnd
              val epochOptAlloc  = prevOptProgramBudget0 / epochNumToEnd

              val allocMainRem = poolMainReward0 - epochMainAlloc * epochsToCompound
              val allocOptRem  = poolOptReward0 - epochOptAlloc * epochsToCompound

              val rewardMain: Long =
                if (actualTMP > 0) {
                  ((allocMainRem - 1L) * releasedTMP.toBigInt / actualTMP).toLong
                } else 0L
              val rewardOpt: Long =
                if (actualTMP > 0) {
                  ((allocOptRem - 1L) * releasedTMP.toBigInt / actualTMP).toLong
                } else 0L

              // ===== Validate compounding ===== //
              // Valid TMP And bundleKey:
              val validTMPAndKey = if (epochsToCompound > 0) {
                val bundleTMP1 = successor.tokens(1)

                (bundleKey0, 1L) == successor.tokens(2) &&
                (bundleTMP1._1 == bundleTMP0._1) &&
                (bundleTMP - bundleTMP1._2 == releasedTMP)

              } else {
                (bundleKey0, 1L) == successor.tokens(1)
              }
              // Valid Successor:
              val validSuccessor =
                (successor.R6[Coll[Byte]].get == poolId0) &&
                (successor.propositionBytes == SELF.propositionBytes) &&
                (bundleVLQ1 == bundleVLQ0)

              // Valid Reward:
              val validReward = {
                // Here we must take into account 8 possible cases:
                // 1. First compounding (parallel rewards): Bundle0(vLQ0, BundleKeyToken0, TMP0) ->
                //                                          Bundle1(vLQ1, BundleKeyToken1, TMP1, rewardMain1, rewardOpt1);
                //
                // 2. First compounding (only main rewards): Bundle0(vLQ0, BundleKeyToken0, TMP0) ->
                //                                           Bundle1(vLQ1, BundleKeyToken1, TMP1, rewardMain1);
                //
                // 3. Normal compounding (parallel rewards): Bundle0(vLQ0, BundleKeyToken0, TMP0, rewardMain0, rewardOpt0) ->
                //                                           Bundle1(vLQ1, BundleKeyToken1, TMP1, rewardMain1, rewardOpt1);
                //
                // 4. Normal compounding (only main rewards): Bundle0(vLQ0, BundleKeyToken0, TMP0, rewardMain0) ->
                //                                            Bundle1(vLQ1, BundleKeyToken1, TMP1, rewardMain1);
                //
                // 5. Last compounding (parallel rewards): Bundle0(vLQ0, BundleKeyToken0, TMP0, rewardMain0, rewardOpt0) ->
                //                                         Bundle1(vLQ1, BundleKeyToken1, rewardMain1, rewardOpt1);
                //
                // 6. Last compounding (only main rewards): Bundle0(vLQ0, BundleKeyToken0, TMP0, rewardMain0) ->
                //                                          Bundle1(vLQ1, BundleKeyToken1, rewardMain1);
                //
                // 7. First compounding with added opt rewards:      Bundle0(vLQ0, BundleKeyToken0, TMP0, rewardMain0) ->
                //                                                   Bundle1(vLQ1, BundleKeyToken1, TMP1, rewardMain1, rewardOpt1);

                val maxRoundingError0 = pool0.R7[Long].get

                val isParallelRewards  = if (prevOptProgramBudget0 > maxRoundingError0) true else false
                val isEqualBundlesSize = SELF.tokens.size == successor.tokens.size
                val isLastCompounding  = epochsToCompound == 0

                val isFirstParallel = isParallelRewards && (SELF.tokens.size == 3) && (successor.tokens.size == 5)
                val isFirstNormal   = !isParallelRewards && (SELF.tokens.size == 3)

                val mainRewardIsZero = rewardMain == 0
                val optRewardIsZero  = rewardOpt == 0

                // Case 1:
                val isFirstNormalParallel = isFirstParallel && !mainRewardIsZero && !optRewardIsZero
                // Case 2:
                val isFirstNormalMain = isFirstNormal && !mainRewardIsZero
                // Case 3:
                val isNormalParallel =
                  isParallelRewards && isEqualBundlesSize && !mainRewardIsZero && !optRewardIsZero
                // Case 4:
                val isNormalMain =
                  !isParallelRewards && isEqualBundlesSize && !mainRewardIsZero
                // Case 5:
                val isLastParallel =
                  isParallelRewards && !isEqualBundlesSize && !mainRewardIsZero && !optRewardIsZero && isLastCompounding
                // Case 6:
                val isLastMain = !isParallelRewards && !isEqualBundlesSize && !mainRewardIsZero
                // Case 7:
                val isFirstAddedOpt =
                  isParallelRewards && !isEqualBundlesSize && !mainRewardIsZero && !optRewardIsZero && !isLastCompounding && (SELF.tokens.size == 4)

                // Calculating bundle rewards deltas:
                val mainRewardZero = (poolMainRewardToken0._1, 0L)
                val optRewardZero  = (poolOptRewardToken0._1, 0L)

                val rewardMain0: (Coll[Byte], Long) =
                  if (isFirstNormalParallel || isFirstNormalMain) {
                    mainRewardZero
                  } else SELF.tokens(3) // (isNormalParallel || isNormalMain || isLastParallel ||
                // isLastMain || isFirstAddedOpt)

                val rewardOpt0: (Coll[Byte], Long) =
                  if (isNormalParallel || isLastParallel) {
                    SELF.tokens(4)
                  } else
                    optRewardZero // (isFirstNormalParallel || isFirstNormalMain ||
                // || isNormalMain || isLastMain || isFirstAddedOpt)

                val rewardMain1: (Coll[Byte], Long) =
                  if (isFirstNormalParallel || isFirstNormalMain || isNormalParallel || isNormalMain || isFirstAddedOpt)
                    successor.tokens(3)
                  else if (isLastParallel || isLastMain)
                    successor.tokens(2)
                  else mainRewardZero

                val rewardOpt1: (Coll[Byte], Long) =
                  if (isFirstNormalParallel || isNormalParallel || isFirstAddedOpt)
                    successor.tokens(4)
                  else if (isLastParallel)
                    successor.tokens(3)
                  else optRewardZero //  (isNormalMain || isLastMain)

                (rewardMain1._1 == pool0.tokens(1)._1) &&
                (rewardOpt1._1 == pool0.tokens(5)._1) &&
                (rewardMain1._2 - rewardMain0._2 >= rewardMain - 1L) &&
                (rewardOpt1._2 - rewardOpt0._2 >= rewardOpt - 1L)
              }

              validReward &&
              validSuccessor &&
              validTMPAndKey

            } else if (deltaLQ < 0L) { // Redeem.
              // ===== Getting INPUTS data ===== //
              val bundleKeyId    = if (epochsToCompound > 0) SELF.tokens(2)._1 else SELF.tokens(1)._1
              val permitIn       = INPUTS(2)
              val requiredPermit = (bundleKeyId, 0x7fffffffffffffffL - 1L)

              // Check if all rewards are redeemed:
              val selfSize = SELF.tokens.size
              val rewardsAreEmpty = if (epochsToCompound > 0 && selfSize == 5) {
                SELF.tokens(3)._2 == 1 && SELF.tokens(4)._2 == 1
              } else if (epochsToCompound > 0 && selfSize == 4) { SELF.tokens(3)._2 == 1 }
              else if (epochsToCompound == 0 && selfSize == 4) { SELF.tokens(2)._2 == 1 && SELF.tokens(3)._2 == 1 }
              else if (epochsToCompound == 0 && selfSize == 3) { SELF.tokens(2)._2 == 1 }
              else false

              // ===== Validate redeem ===== //
              // Valid BundleKeyId tokens provided:
              permitIn.tokens(0) == requiredPermit &&
              rewardsAreEmpty

            } else false

          validPool &&
          validAction

        } else { // Redeem rewards.
          // ===== Getting INPUTS data ===== //
          val permitIn = INPUTS(1)

          val bundleKeyTokenInd = getVar[Int](0).get
          val rewardInd         = getVar[Int](1).get

          val bundleKeyId    = SELF.tokens(bundleKeyTokenInd)._1
          val requiredPermit = (bundleKeyId, 0x7fffffffffffffffL - 1L)

          // ===== Getting OUTPUTS data ===== //
          val bundleOut = OUTPUTS(0)

          // Valid BundleKeyId tokens provided:
          val validRedeemer = permitIn.tokens(0) == requiredPermit

          // Out Bundle is valid:
          val validSuccessor = {
            (bundleOut.R6[Coll[Byte]].get == SELF.R6[Coll[Byte]].get) &&
            (bundleOut.propositionBytes == SELF.propositionBytes) &&
            (bundleOut.tokens(rewardInd)._2 >= 1)
          }

          validRedeemer &&
          validSuccessor

        }
      }
      sigmaProp(validStateTransition)
    }
}

object StakingBundleBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): StakingBundleBox[F] =
    new StakingBundleBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[StakingBundleBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
