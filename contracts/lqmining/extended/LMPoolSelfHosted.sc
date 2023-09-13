{ // ===== Contract Information ===== //
  // Name: LMPoolSelfHostedExtended.
  // Description: Contract that validates a change in the extended self-hosted LM pool's state.
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
  //      0: Main program budget;         // Last updated main reward tokens amount after budget deposit/redeem.
  //      1: Optional program budget;     // Last updated optional reward tokens amount after budget deposit/redeem.
  //   R6[Coll[Coll[Bytes]]]: Redeemers:
  //      0: Main SigmaPropBytes;         // Address which can redeem main program budget.
  //      1: Optional SigmaPropBytes;     // Address which can redeem optional program budget.
  //   R7[Long]: Max Rounding Error.      // Reward tokens rounding delta max value.
  //   R8[Int]: Epoch index.              // index of the epoch being compounded (required only for compounding).
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
  // {31}  -> BundleScriptHash[Coll[Byte]]
  //
  // Epoch Budgets Update Tx:
  //    INPUTS:  (0 -> pool_in).
  //    OUTPUTS: (0 -> pool_out).
  //
  // ErgoTree: 19c807340400040004020402040404040406040604080408040a040a040404020400040004020402040204020400040004020402040001010101040c0500050004040e207dea0cba5791f44220d3ecf272c137a233c5b5060be2e6dcf04e647219e20e57040004020500040605000500050005feffffffffffffffff01050004020500050005000500050004000402010001000100d828d601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d6068cc7a701d607b27202730200d608b27203730300d609b27202730400d60ab27203730500d60bb27202730600d60cb27203730700d60d8c720c01d60eb27202730800d60fb27203730900d6108c720f01d611b27202730a00d612b27203730b00d6138c720a02d614998c7209027213d615998c720b028c720c02d6168c720802d617e4c6a70705d6187e721706d619b27205730c00d61a9a99a37219730dd61bb27205730e00d61c9d721a721bd61d95919e721a721b730f9a721c7310721cd61eb27205731100d61f958f721d721e9a99721e9590721d721e721d9a721e731273137314d6207e721f05d621e4c6a70511d622b27221731500d6237e9a721f731605d6248c721202d625b27221731700d626ed929a7e72160672187e9c72209d7222722306929a7e72240672187e9c72209d7225722306d6278c720f02d628998c720e027227d1edededededed93b272027318007204eded93e4c672010410720573199272067206731a93c27201c2a7edededed938c7207018c720801938c7209018c720a01938c720b01720d938c720e017210938c7211018c72120190b17202731b95917214731cd802d6299c7214997e721e05a2731d7e721d05d62ab2a5731e00ededed7226937214f07215937229f07228edededed93cbc2722a731f938602720d7214b2db6308722a73200093860272107229b2db6308722a732100e6c6722a060893e4c6722a070e8c720401958f72147322ededec722692a39a9a72199c721e721bb27205732300937215f072149272289591721d721e73249c7215997e721e05a273257e721d05d802d629998c7207027216d62a998c7211027224959172287326d802d62be4c672010804d62c9999732772279c72137e99721e722b059591722c7328ededededed90722b99721d7329722690f072299d9c9972169c722272207228722c90f0722a9d9c9972249c722572207228722c937214732a937215732b95937228732cd801d62de4c67201051195ecec947229732d94722a732eec92997216722272179299722472257217eded93b2722d732f00721693b2722d73300072247226733173327333
  //
  // ErgoTreeTemplate: d828d601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d6068cc7a701d607b27202730200d608b27203730300d609b27202730400d60ab27203730500d60bb27202730600d60cb27203730700d60d8c720c01d60eb27202730800d60fb27203730900d6108c720f01d611b27202730a00d612b27203730b00d6138c720a02d614998c7209027213d615998c720b028c720c02d6168c720802d617e4c6a70705d6187e721706d619b27205730c00d61a9a99a37219730dd61bb27205730e00d61c9d721a721bd61d95919e721a721b730f9a721c7310721cd61eb27205731100d61f958f721d721e9a99721e9590721d721e721d9a721e731273137314d6207e721f05d621e4c6a70511d622b27221731500d6237e9a721f731605d6248c721202d625b27221731700d626ed929a7e72160672187e9c72209d7222722306929a7e72240672187e9c72209d7225722306d6278c720f02d628998c720e027227d1edededededed93b272027318007204eded93e4c672010410720573199272067206731a93c27201c2a7edededed938c7207018c720801938c7209018c720a01938c720b01720d938c720e017210938c7211018c72120190b17202731b95917214731cd802d6299c7214997e721e05a2731d7e721d05d62ab2a5731e00ededed7226937214f07215937229f07228edededed93cbc2722a731f938602720d7214b2db6308722a73200093860272107229b2db6308722a732100e6c6722a060893e4c6722a070e8c720401958f72147322ededec722692a39a9a72199c721e721bb27205732300937215f072149272289591721d721e73249c7215997e721e05a273257e721d05d802d629998c7207027216d62a998c7211027224959172287326d802d62be4c672010804d62c9999732772279c72137e99721e722b059591722c7328ededededed90722b99721d7329722690f072299d9c9972169c722272207228722c90f0722a9d9c9972249c722572207228722c937214732a937215732b95937228732cd801d62de4c67201051195ecec947229732d94722a732eec92997216722272179299722472257217eded93b2722d732f00721693b2722d73300072247226733173327333
  //
  // ErgoTreeTemplateHash: f167fadc55d0531abda719d096a15b919b982725d15a5b47295cb4b30f9544dd
  //
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

  val budgetRedeemers0  = SELF.R6[Coll[SigmaProp]].get
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

  val budgetRedeemers1  = SELF.R6[Coll[SigmaProp]].get
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

    // Check if previous epochs are fully compounded (is needed for different actions):
    val epochNumToEnd =
      if (curEpochIx < 2) epochNum
      else if (curEpochIx >= 2 && curEpochIx < epochNum) epochNum - curEpochIxR + 1
      else 1

    val epochMainAlloc0 = prevMainProgramBudget0 / epochNumToEnd
    val epochOptAlloc0  = prevOptProgramBudget0 / epochNumToEnd

    val virtualMainAllocation0 = epochNumToEnd * epochMainAlloc0.toBigInt - reservesMainReward
    val virtualOptAllocation0  = epochNumToEnd * epochOptAlloc0.toBigInt - reservesOptReward

    val validOptBudget0 = if (virtualOptAllocation0 > maxRoundingError0) {

      (virtualOptAllocation0 >= -maxRoundingError0) &&
      (virtualOptAllocation0 <= epochOptAlloc0)

    } else true

    // prevEpochsCompounded:
    val prevEpochsCompounded = {
      curEpochIx <= 1 || (validOptBudget0 && (virtualMainAllocation0 + maxRoundingError0 >= 0 &&
      (virtualMainAllocation0 >= maxRoundingError0) &&
      (virtualMainAllocation0 <= epochMainAlloc0)))
    }

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
        bundleOut.R6[SigmaProp].isDefined &&
        bundleOut.R7[Coll[Byte]].get == poolNFT0._1

      prevEpochsCompounded &&
      deltaLQ == -deltaVLQ &&
      releasedTMP == -deltaTMP &&
      validBundle

    } else if (deltaLQ < 0) { // Redeem.
      val releasedLQ = deltaVLQ
      val minReturnedTMP =
        if (curEpochIx > epochNum) 0L
        else {
          val epochsDeallocated = epochNum - max(0L, curEpochIx)
          releasedLQ * epochsDeallocated
        }

      val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

      (prevEpochsCompounded || redeemNoLimit) &&
      (deltaVLQ == -deltaLQ) &&
      (deltaTMP >= minReturnedTMP)

    } else {

      val deltaMainReward = poolMainReward1._2 - reservesMainReward
      val deltaOptReward  = poolOptReward1._2 - reservesOptReward

      if (deltaTMP > 0) // Compound.
        {
          // 7.3.
          val epoch                 = successor.R8[Int].get
          val epochsToCompound      = epochNum - epoch
          val epochMainAlloc        = prevMainProgramBudget0 / (epochsToCompound + 1)
          val epochOptAlloc         = prevOptProgramBudget0 / (epochsToCompound + 1)
          val virtualMainAllocation = (epochsToCompound + 1) * epochMainAlloc.toBigInt - reservesMainReward
          val virtualOptAllocation  = (epochsToCompound + 1) * epochOptAlloc.toBigInt - reservesOptReward

          // Optional budget is correct:
          val validOptBudget = if (virtualOptAllocation > maxRoundingError0) {

            (virtualOptAllocation >= maxRoundingError0) &&
            (virtualOptAllocation <= epochOptAlloc)

          } else true

          // Check if previous epoch is fully compounded:
          val prevEpochsCompoundedCorrected = {
            epoch <= 1 || (validOptBudget && (virtualMainAllocation + maxRoundingError0 >= 0 &&
            (virtualMainAllocation >= -maxRoundingError0) &&
            (virtualMainAllocation <= epochMainAlloc)))
          }

          // Epoch is legal:
          val legalEpoch = epoch <= curEpochIx - 1

          val actualTMP    = 0x7fffffffffffffffL - poolTMP0._2 - reservesLQ * epochsToCompound
          val allocMainRem = reservesMainReward - epochMainAlloc * epochsToCompound
          val allocOptRem  = reservesOptReward - epochOptAlloc * epochsToCompound

          if (actualTMP > 0) {
            val rewardMain = allocMainRem * deltaTMP / actualTMP
            val rewardOpt  = allocOptRem * deltaTMP / actualTMP

            legalEpoch &&
            prevEpochsCompoundedCorrected &&
            (-deltaMainReward <= rewardMain) &&
            (-deltaOptReward <= rewardOpt) &&
            (deltaLQ == 0L) &&
            (deltaVLQ == 0L)

          } else false
        } else if (deltaTMP == 0) {

        val budgets1 = successor.R5[Coll[Long]].get

        val prevMainProgramBudget1 = budgets1(0)
        val prevOptProgramBudget1  = budgets1(1)

        // Calculate if pool stores correct budgets:
        val budgetsNotUpdated = ((reservesMainReward - prevMainProgramBudget0) >= maxRoundingError0) ||
          ((reservesOptReward - prevOptProgramBudget0) >= maxRoundingError0)

        if (
          (deltaMainReward > 0 || deltaOptReward > 0 || !budgetsNotUpdated) && (poolMainReward1._2 != 0 && poolOptReward0._2 != 0)
        ) // main/optional Budget Deposit/Epoch Budgets Update.
          {

            (prevMainProgramBudget1 == poolMainReward1._2) &&
            (prevOptProgramBudget1 == poolOptReward1._2) &&
            prevEpochsCompounded

          } else if (poolMainReward1._2 == 0 || poolOptReward0._2 == 0) // main/optional Budget Redeem.
          {
            // Budget Redeem is valid only after program is ended:
            val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

            redeemNoLimit

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
