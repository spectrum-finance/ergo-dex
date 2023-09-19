{ // ===== Contract Information ===== //
  // Name: LMPoolSelfHostedExtended.
  // Description: Contract that validates a change in the parallel self-hosted LM pool's state.
  //
  // ===== LM Pool Box ===== //
  // Registers:
  //   R4[Coll[Int]]: LM program config:
  //      0: Length of every epoch in blocks;
  //      1: Number of epochs in the LM program;
  //      2: Program start;
  //      3: Redeem blocks delta.         // the number of blocks after the end of LM program, at which redeems can\
  //                                      // be performed without any restrictions.
  //   R5[Coll[Long]]: Program budgets:
  //      0: Main program budget;         // Last updated main reward tokens amount.
  //      1: Optional program budget.     // Last updated optional reward tokens amount.
  //   R6[Coll[Coll[Byte]]: Redeemers:
  //      0: Main SigmaPropBytes;         // Address which can redeem main program budget.
  //      1: Optional SigmaPropBytes.     // Address which can redeem optional program budget.
  //   R7[Long]: Max Rounding Error.      // Reward tokens rounding delta max value.
  //   R8[Int]: Epoch index.              // index of the last compounded epoch.
  //
  // Tokens:
  //   0:
  //     _1: LM Pool NFT
  //     _2: Amount: 1
  //   1:
  //     _1: Main reward Token ID.
  //     _2: Amount of main reward tokens.
  //   2:
  //     _1: LQ Token ID  // locked LQ tokens.
  //     _2: Amount of LQ tokens.
  //   3:
  //     _1: vLQ Token ID  // tokens representing locked share of LQ.
  //     _2: Amount of vLQ tokens.
  //   4:
  //     _1: TMP Token ID  // left program epochs times liquidity.
  //     _2: Amount of TMP tokens.
  //   5:
  //     _1: Optional reward Token ID.
  //     _2: Amount of optional reward tokens.
  //
  // Constants:
  // {33}  -> BundleScriptHash[Coll[Byte]]
  //
  // Epoch Budgets Update Tx:
  //    INPUTS:  (0 -> pool_in).
  //    OUTPUTS: (0 -> pool_out).
  //
  // ErgoTree: 19fe09410400040004020402040404040406040604080408040a040a040204040402040004000402040201010402040604000402060101040001010101040c05000500040405040e2029b1a34e0ec368524887d8565e352a9f063ce51b0f608fd24aee16a11e4665a6040004020500050005000500040205feffffffffffffffff0104020500040205000500010005000502050005000400040204020402040204040402040205000400040201000100d82dd601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d6068cc7a701d607b27202730200d608b27203730300d609b27202730400d60ab27203730500d60bb27202730600d60cb27203730700d60d8c720c01d60eb27202730800d60fb27203730900d6108c720f01d611b27202730a00d612b27203730b00d6138c720a02d614998c7209027213d615b27205730c00d616b27205730d00d6179a99a37216730ed618b27205730f00d6199d72177218d61a95919e7217721873109a721973117219d61b90721a7312d61ce4c6a70804d61d95721b731393a1721599721a7314721cd61ee4c672010804d61f93721e721cd620998c720b028c720c02d6218c720f02d622998c720e027221d623b27205731500d6248c720702d6258c720802d6268c721102d6278c721202d628e4c6a70511d629b27228731600d62ab27228731700d62b7318d62ce4c6a70705d62de4c6a7061ad1edededededed93b272027319007204eded93e4c6720104107205731a9272067206731b93c27201c2a7edededed938c7207018c720801938c7209018c720a01938c720b01720d938c720e017210938c7211018c72120190b17202731c95917214731dd803d62e997e721505a2731e7e721a05d62f9c7214722ed630b2a5731f00ededededed721d92722e7320721f937214f0722093722ff07222ededed93cbc272307321938602720d7214b2db630872307322009386027210722fb2db6308723073230093e4c67230060e8c720401958f72147324edededec721d92a39a9a72169c721572187223721f937220f072149272229591721a721573259c7220997e721505a273267e721a05d802d62e9972247225d62f9972267227959172227327d805d6309a721c7328d6319972157230d6327e723105d6339999732972219c72137232d6347e9a7231732a0595917233732bd802d6357e722206d6367e723306ededededed90723099721a732c721f907ef0722e069d9c997e9972259c9d72297234723206722b72357236907ef0722f069d9c997e9972279c9d722a7234723206722b72357236937214732d937220732e732f959372227330d807d630f0722cd63193722a7331d632ec9099722572297230edef723190997227722a7230d633ed94722473329472277333d634e4c672010511d635b27234733400d636b2723473350095ededec91722e722c91722f722cef72327233edededed8f721a721593723572249372367226721d721f95ed72327233d803d63795721b721595ed91721a73368f721a72159a997215721973377338d6389972297225d63999722a7227edededec90721a7339edec7231ed9272399c72307e723705909c7e7239067e7237067e722a06ed9272389c72307e723705909c7e7238067e7237067e7229069372357224937236722693721e9a721c733a95edef7233ef7232d801d637b2a5733b00ed958f722e733c93c27237b2722d733d0093c27237b2722d733e0092a39a9a72169c721572187223733f7340
  //
  // ErgoTreeTemplate: d82dd601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d6068cc7a701d607b27202730200d608b27203730300d609b27202730400d60ab27203730500d60bb27202730600d60cb27203730700d60d8c720c01d60eb27202730800d60fb27203730900d6108c720f01d611b27202730a00d612b27203730b00d6138c720a02d614998c7209027213d615b27205730c00d616b27205730d00d6179a99a37216730ed618b27205730f00d6199d72177218d61a95919e7217721873109a721973117219d61b90721a7312d61ce4c6a70804d61d95721b731393a1721599721a7314721cd61ee4c672010804d61f93721e721cd620998c720b028c720c02d6218c720f02d622998c720e027221d623b27205731500d6248c720702d6258c720802d6268c721102d6278c721202d628e4c6a70511d629b27228731600d62ab27228731700d62b7318d62ce4c6a70705d62de4c6a7061ad1edededededed93b272027319007204eded93e4c6720104107205731a9272067206731b93c27201c2a7edededed938c7207018c720801938c7209018c720a01938c720b01720d938c720e017210938c7211018c72120190b17202731c95917214731dd803d62e997e721505a2731e7e721a05d62f9c7214722ed630b2a5731f00ededededed721d92722e7320721f937214f0722093722ff07222ededed93cbc272307321938602720d7214b2db630872307322009386027210722fb2db6308723073230093e4c67230060e8c720401958f72147324edededec721d92a39a9a72169c721572187223721f937220f072149272229591721a721573259c7220997e721505a273267e721a05d802d62e9972247225d62f9972267227959172227327d805d6309a721c7328d6319972157230d6327e723105d6339999732972219c72137232d6347e9a7231732a0595917233732bd802d6357e722206d6367e723306ededededed90723099721a732c721f907ef0722e069d9c997e9972259c9d72297234723206722b72357236907ef0722f069d9c997e9972279c9d722a7234723206722b72357236937214732d937220732e732f959372227330d807d630f0722cd63193722a7331d632ec9099722572297230edef723190997227722a7230d633ed94722473329472277333d634e4c672010511d635b27234733400d636b2723473350095ededec91722e722c91722f722cef72327233edededed8f721a721593723572249372367226721d721f95ed72327233d803d63795721b721595ed91721a73368f721a72159a997215721973377338d6389972297225d63999722a7227edededec90721a7339edec7231ed9272399c72307e723705909c7e7239067e7237067e722a06ed9272389c72307e723705909c7e7238067e7237067e7229069372357224937236722693721e9a721c733a95edef7233ef7232d801d637b2a5733b00ed958f722e733c93c27237b2722d733d0093c27237b2722d733e0092a39a9a72169c721572187223733f7340
  //
  // ErgoTreeTemplateHash: 1fcdd8aeabc6527fbdf793004c03145335b36e3da938b5a5ea7808bf183d9c84
  //
  // ===== Getting INPUTS data ===== //
  // ===== Getting INPUTS data ===== //
  val creationHeight0 = SELF.creationInfo._1

  val poolNFT0        = SELF.tokens(0)
  val poolMainReward0 = SELF.tokens(1)
  val poolLQ0         = SELF.tokens(2)
  val poolVLQ0        = SELF.tokens(3)
  val poolTMP0        = SELF.tokens(4)
  val poolOptReward0  = SELF.tokens(5)

  val conf0            = SELF.R4[Coll[Int]].get
  val epochLen         = conf0(0)
  val epochNum         = conf0(1)
  val programStart     = conf0(2)
  val redeemLimitDelta = conf0(3)

  val budgets0               = SELF.R5[Coll[Long]].get
  val prevMainProgramBudget0 = budgets0(0)
  val prevOptProgramBudget0  = budgets0(1)

  val budgetRedeemers0  = SELF.R6[Coll[Coll[Byte]]].get
  val maxRoundingError0 = SELF.R7[Long].get

  // ===== Getting OUTPUTS data ===== //
  val successor = OUTPUTS(0)

  val creationHeight1 = SELF.creationInfo._1

  val poolNFT1        = successor.tokens(0)
  val poolMainReward1 = successor.tokens(1)
  val poolLQ1         = successor.tokens(2)
  val poolVLQ1        = successor.tokens(3)
  val poolTMP1        = successor.tokens(4)
  val poolOptReward1  = successor.tokens(5)

  val conf1 = successor.R4[Coll[Int]].get

  val budgetRedeemers1  = SELF.R6[Coll[Coll[Byte]]].get
  val maxRoundingError1 = SELF.R7[Long].get

  // ===== Getting deltas ===== //
  val reservesMainReward = poolMainReward0._2
  val reservesOptReward  = poolOptReward0._2
  val reservesLQ         = poolLQ0._2

  val deltaLQ  = poolLQ1._2 - reservesLQ
  val deltaVLQ = poolVLQ1._2 - poolVLQ0._2
  val deltaTMP = poolTMP1._2 - poolTMP0._2

  // ===== Calculating epoch parameters ===== //
  val curBlockIx    = HEIGHT - programStart + 1
  val curEpochIxRem = curBlockIx % epochLen
  val curEpochIxR   = curBlockIx / epochLen
  val curEpochIx    = if (curEpochIxRem > 0) curEpochIxR + 1 else curEpochIxR

  // ===== Validating conditions ===== //
  // Pool NFT is preserved:
  val nftPreserved = poolNFT1 == poolNFT0
  // Config is preserved:
  val configPreserved =
    (conf1 == conf0) &&
    (maxRoundingError1 == maxRoundingError0) &&
    (creationHeight1 >= creationHeight0)
  // Budget redeemers are preserved:
  val budgetRedeemersPreserved = budgetRedeemers1 == budgetRedeemers0
  // Script is preserved:
  val scriptPreserved = successor.propositionBytes == SELF.propositionBytes
  // Pool tokens are preserved:
  val assetsPreserved =
    poolMainReward1._1 == poolMainReward0._1 &&
    poolLQ1._1 == poolLQ0._1 &&
    poolVLQ1._1 == poolVLQ0._1 &&
    poolTMP1._1 == poolTMP0._1 &&
    poolOptReward1._1 == poolOptReward0._1
  // No more tokens check:
  val noMoreTokens = successor.tokens.size <= 6
  // Validate action:
  val validAction = {

    val lastCompoundedEpoch0         = SELF.R8[Int].get
    val lastCompoundedEpoch1         = successor.R8[Int].get
    val lastCompoundedEpochPreserved = lastCompoundedEpoch1 == lastCompoundedEpoch0

    // Check if previous epochs are fully compounded (is needed for different actions):
    val prevEpochsCompounded = if (curEpochIx <= 1) { true }
    else { min(epochNum, curEpochIx - 1) == lastCompoundedEpoch0 }

    if (deltaLQ > 0) { // Deposit.
      val releasedVLQ     = deltaLQ
      val epochsAllocated = epochNum - max(0L, curEpochIx)
      val releasedTMP     = releasedVLQ * epochsAllocated

      val bundleOut = OUTPUTS(2)
      // Out Bundle is valid:
      val validBundle =
        blake2b256(bundleOut.propositionBytes) == BundleScriptHash &&
        (poolVLQ0._1, releasedVLQ) == bundleOut.tokens(0) &&
        (poolTMP0._1, releasedTMP) == bundleOut.tokens(1) &&
        bundleOut.R6[Coll[Byte]].get == poolNFT0._1

      prevEpochsCompounded &&
      epochsAllocated >= 2 &&
      lastCompoundedEpochPreserved &&
      deltaLQ == -deltaVLQ &&
      releasedTMP == -deltaTMP &&
      validBundle

    } else if (deltaLQ < 0) // Redeem.
      {
        val releasedLQ = deltaVLQ
        val minReturnedTMP =
          if (curEpochIx > epochNum) 0L
          else {
            val epochsDeallocated = epochNum - max(0L, curEpochIx)
            releasedLQ * epochsDeallocated
          }

        val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

        (prevEpochsCompounded || redeemNoLimit) &&
        lastCompoundedEpochPreserved &&
        (deltaVLQ == -deltaLQ) &&
        (deltaTMP >= minReturnedTMP)

      } else {

      val deltaMainReward = poolMainReward1._2 - reservesMainReward
      val deltaOptReward  = poolOptReward1._2 - reservesOptReward

      if (deltaTMP > 0) // Compound.
        {
          val epoch            = lastCompoundedEpoch0 + 1
          val epochsToCompound = epochNum - epoch
          val epochMainAlloc   = prevMainProgramBudget0 / (epochsToCompound + 1)
          val epochOptAlloc    = prevOptProgramBudget0 / (epochsToCompound + 1)

          // Epoch is legal:
          val legalEpoch = epoch <= curEpochIx - 1

          val actualTMP    = 0x7fffffffffffffffL - poolTMP0._2 - reservesLQ * epochsToCompound
          val allocMainRem = reservesMainReward - epochMainAlloc * epochsToCompound
          val allocOptRem  = reservesOptReward - epochOptAlloc * epochsToCompound

          if (actualTMP > 0) {
            val rewardMain = (allocMainRem.toBigInt - 1L) * deltaTMP / actualTMP
            val rewardOpt  = (allocOptRem.toBigInt - 1L) * deltaTMP / actualTMP

            legalEpoch &&
            lastCompoundedEpochPreserved &&
            (-deltaMainReward <= rewardMain) &&
            (-deltaOptReward <= rewardOpt) &&
            (deltaLQ == 0L) &&
            (deltaVLQ == 0L)

          } else false
        } else if (deltaTMP == 0) // Other actions (Epoch Budgets Update and Budget Deposit/Redeem):
        {

          val budgets1 = successor.R5[Coll[Long]].get

          val prevMainProgramBudget1 = budgets1(0)
          val prevOptProgramBudget1  = budgets1(1)

          // Mark if only main reward is available:
          val optBudgetIsNotInit = prevOptProgramBudget0 == 1L

          // Calculate if pool stores correct budgets:
          val budgetsNotUpdated =
            (reservesMainReward - prevMainProgramBudget0 <= -maxRoundingError0) ||
            (!optBudgetIsNotInit && (reservesOptReward - prevOptProgramBudget0 <= -maxRoundingError0))

          val budgetsNotDepleted = poolMainReward1._2 != 0 && poolOptReward0._2 != 0

          if (
            (deltaMainReward > maxRoundingError0 || deltaOptReward > maxRoundingError0) &&
            !budgetsNotUpdated && budgetsNotDepleted
          ) // main/optional Budget Deposit.
            {
              (curEpochIx < epochNum) &&
              (prevMainProgramBudget1 == poolMainReward1._2) &&
              (prevOptProgramBudget1 == poolOptReward1._2) &&
              prevEpochsCompounded &&
              lastCompoundedEpochPreserved

            } else if (budgetsNotUpdated && budgetsNotDepleted) // Epoch Budgets Update.
            {
              // Check if previous epoch is fully compounded:
              val epochNumToEnd =
                if (curEpochIx <= 1) epochNum
                else if (curEpochIx > 1 && curEpochIx < epochNum) epochNum - curEpochIxR + 1
                else 1

              val virtualMainAllocation0 = prevMainProgramBudget0 - reservesMainReward
              val virtualOptAllocation0  = prevOptProgramBudget0 - reservesOptReward
              val validOptBudget0 =
                optBudgetIsNotInit || ((virtualOptAllocation0 >= -maxRoundingError0 * epochNumToEnd) &&
                (virtualOptAllocation0.toBigInt * epochNumToEnd <= prevOptProgramBudget0))

              // prevEpochsCompounded:
              val prevEpochCompounded = {
                curEpochIx <= 2 || (validOptBudget0 && (virtualMainAllocation0 >= -maxRoundingError0 * epochNumToEnd &&
                (virtualMainAllocation0.toBigInt * epochNumToEnd <= prevMainProgramBudget0)))
              }

              prevEpochCompounded &&
              (prevMainProgramBudget1 == poolMainReward1._2) &&
              (prevOptProgramBudget1 == poolOptReward1._2) &&
              (lastCompoundedEpoch1 == lastCompoundedEpoch0 + 1)

            } else if (!budgetsNotDepleted && !budgetsNotUpdated) // main/optional Budget Redeem.
            {
              // Check if budget redeemer is valid:
              val redeemerBudgetOut = OUTPUTS(1)
              val validRedeemer =
                if (deltaMainReward < 0) redeemerBudgetOut.propositionBytes == budgetRedeemers0(0)
                else
                  redeemerBudgetOut.propositionBytes == budgetRedeemers0(1)

              // Budget Redeem is valid only after program is ended:
              val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

              validRedeemer && redeemNoLimit

            } else false
        } else false
    }
  }
  sigmaProp(
    nftPreserved &&
    configPreserved &&
    budgetRedeemersPreserved &&
    scriptPreserved &&
    assetsPreserved &&
    noMoreTokens &&
    validAction
  )
}
