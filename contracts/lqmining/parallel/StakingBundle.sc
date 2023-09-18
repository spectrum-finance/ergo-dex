{ // ===== Contract Information ===== //
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
  //  {0} -> actionId[Int]  // 0 - deposit/compound/redeem;
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
  // ErgoTree: 19c409530416040004000400040404040402040204000400040004020400040a0400050004020101010005feffffffffffffffff01040805000402040204000502050005000502050005000406040a04060400040804040500040605000408040604040406050204080502040004000402050205020402050004000404040404000404040205fcffffffffffffffff01040a040605020408050204080406050204000408050204060502040605020100010004000402040005fcffffffffffffffff010502d1959373007301d80fd601db6308b2a5730200d602e4c6a7060ed603b2a4730300d604db63087203d6058cb2720473040002d606998cb27201730500027205d607db6308a7d6089ae4c6720308047306d609b2e4c672030410730700d60a959072087308720995ed91720873098f720872099972097208730ad60bb27204730b00d60cb27207730c00d60db27204730d00d60e8c720b01d60f8c720d01ed938cb27201730e0001720295937206730fd820d610e4c672030511d611b27210731000d61295917211e4c67203070573117312d613b17207d614b2a5e4e3010400d615db63087214d616b17215d6177e720a05d618999973138cb27204731400029c72057217d6199172187315d61a7e9a720a731605d61bb27207731700d61c8c721b02d61d99721c9c72178c720c02d61e9572197d9d9c7e99998c720b029c9db27210731800721a72177319067e721d067e72180605731ad61f93721e731bd6209572197d9d9c7e99998c720d029c9d7211721a7217731c067e721d067e72180605731dd621937220731ed622edededed7212937213731f9372167320ef721fef7221d623ef7212d624eded72239372137321ef721fd6259372137216d626ededed72127225ef721fef7221d62793720a7322d628ededededed7212ef7225ef721fef7221ef72279372137323d629edededed7212ef7225ef721fef72217227d62ab27215732400d62b8602720e7325d62c95ecececec722272247226eded72237225ef721f7228b2721573260095ec7229eded7223ef7225ef7221722a722bd62d8602720f7327d62e95ecec722272267228b27215732800957229b27215732900722dd62f8cb27207732a0001ededededed938c722c01720e938c722e01720f92998c722c028c95ec72227224722bb27207732b000299721e732c92998c722e028c95ec72267229b27207732d00722d02997220732eeded93e4c67214060e720293c27214c2a793b27215732f00720c9591720a7330d801d630b27215733100eded938602722f7332722a938c7230018c721b019399721c8c723002721d938602722f7333b27215733400958f72067335d803d61091720a7336d611b17207d6128cb2720773370002ed93b2db6308b2a473380073390086029572108cb27207733a00018cb27207733b0001733c95ed7210937211733ded938cb27207733e0002733f938cb2720773400002734195ed72109372117342938cb27207734300027344d801d61393720a734595ed72139372117346ed9372127347938cb2720773480002734995ed7213937211734a937212734b734c734dd801d601b2a5734e00ed93b2db6308b2a4734f0073500086028cb2db6308a7e4e3000400017351eded93e4c67201060ee4c6a7060e93c27201c2a7928cb2db63087201e4e3010400027352
  //
  // ErgoTreeTemplate: d1959373007301d80fd601db6308b2a5730200d602e4c6a7060ed603b2a4730300d604db63087203d6058cb2720473040002d606998cb27201730500027205d607db6308a7d6089ae4c6720308047306d609b2e4c672030410730700d60a959072087308720995ed91720873098f720872099972097208730ad60bb27204730b00d60cb27207730c00d60db27204730d00d60e8c720b01d60f8c720d01ed938cb27201730e0001720295937206730fd820d610e4c672030511d611b27210731000d61295917211e4c67203070573117312d613b17207d614b2a5e4e3010400d615db63087214d616b17215d6177e720a05d618999973138cb27204731400029c72057217d6199172187315d61a7e9a720a731605d61bb27207731700d61c8c721b02d61d99721c9c72178c720c02d61e9572197d9d9c7e99998c720b029c9db27210731800721a72177319067e721d067e72180605731ad61f93721e731bd6209572197d9d9c7e99998c720d029c9d7211721a7217731c067e721d067e72180605731dd621937220731ed622edededed7212937213731f9372167320ef721fef7221d623ef7212d624eded72239372137321ef721fd6259372137216d626ededed72127225ef721fef7221d62793720a7322d628ededededed7212ef7225ef721fef7221ef72279372137323d629edededed7212ef7225ef721fef72217227d62ab27215732400d62b8602720e7325d62c95ecececec722272247226eded72237225ef721f7228b2721573260095ec7229eded7223ef7225ef7221722a722bd62d8602720f7327d62e95ecec722272267228b27215732800957229b27215732900722dd62f8cb27207732a0001ededededed938c722c01720e938c722e01720f92998c722c028c95ec72227224722bb27207732b000299721e732c92998c722e028c95ec72267229b27207732d00722d02997220732eeded93e4c67214060e720293c27214c2a793b27215732f00720c9591720a7330d801d630b27215733100eded938602722f7332722a938c7230018c721b019399721c8c723002721d938602722f7333b27215733400958f72067335d803d61091720a7336d611b17207d6128cb2720773370002ed93b2db6308b2a473380073390086029572108cb27207733a00018cb27207733b0001733c95ed7210937211733ded938cb27207733e0002733f938cb2720773400002734195ed72109372117342938cb27207734300027344d801d61393720a734595ed72139372117346ed9372127347938cb2720773480002734995ed7213937211734a937212734b734c734dd801d601b2a5734e00ed93b2db6308b2a4734f0073500086028cb2db6308a7e4e3000400017351eded93e4c67201060ee4c6a7060e93c27201c2a7928cb2db63087201e4e3010400027352
  //
  // ErgoTreeTemplateHash: bf0c44c36c18ec37905ce74d04d67ef587e703c6eca53a52fd6d2cfe1db60be6
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
            val isLastMain = !isParallelRewards && !isEqualBundlesSize && !optRewardIsZero
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
