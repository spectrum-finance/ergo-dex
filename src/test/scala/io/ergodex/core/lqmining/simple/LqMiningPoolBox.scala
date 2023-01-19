package io.ergodex.core.lqmining.simple

import io.ergodex.core.RuntimeState.withRuntimeState
import io.ergodex.core.syntax._
import io.ergodex.core.{BoxSim, RuntimeState}

final class LqMiningPoolBox[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val constants: Map[Int, Any] = Map(23 -> blake2b256("staking_bundle".getBytes().toVector))
) extends BoxSim[F] {
  override val validatorBytes = "lm_pool"

  val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Context (declarations here are only for simulations):
      val BundleScriptHash: Coll[Byte] = getConstant(23).get

      // ===== Contract Information ===== //
      // Name: LMPool
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
      //   R6[Long]: Max Rounding Error // Tokens rounding delta max value.
      //   R7[Long]: Execution budget  // total execution budget.
      //   R8[Int]: Epoch index  // index of the epoch being compounded (required only for compounding).
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
      // Constants:
      // {23}  -> BundleScriptHash[Coll[Byte]]
      //
      // ErgoTree: 19d0062804000400040204020404040404060406040804080404040204000400040204020400040a050005000404040204020e2074aeba0675c10c7fff46d3aa5e5a8efc55f0b0d87393dcb2f4b0a04be213cecb0400040205000402040204060500050005020402050005000500050005000500d820d601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d606e4c6a70505d607e4c6a70705d608e4c6a70605d609b27202730200d60ab27203730300d60bb27202730400d60cb27203730500d60db27202730600d60eb27203730700d60f8c720e01d610b27202730800d611b27203730900d6128c721101d6138c720c02d614998c720b027213d6158c720a02d616b27205730a00d6179a99a37216730bd618b27205730c00d6199d72177218d61a95919e72177218730d9a7219730e7219d61bb27205730f00d61c7e721b05d61d9d7206721cd61e998c720d028c720e02d61f998c7210028c721102d620998c7209027215d1ededededed93b272027310007204edededed93e4c672010410720593e4c672010505720693e4c672010705720793e4c6720106057208928cc77201018cc7a70193c27201c2a7ededed938c7209018c720a01938c720b018c720c01938c720d01720f938c721001721293b172027311959172147312d802d6219c721499721ca273137e721a05d622b2a5731400ededed929a997206721572089c7e999590721a721b721a9a721b7315731605721d937214f0721e937221f0721fedededed93cbc272227317938602720f7214b2db6308722273180093860272127221b2db63087222731900e6c67222040893e4c67222050e8c720401958f7214731aededec929a997206721572089c7e999590721a721b721a9a721b731b731c05721d92a39a9a72169c721b7218b27205731d0093721ef0721492721f9591721a721b731e9c721e99721ca2731f7e721a05d802d621c17201d622c1a7959072217222d802d623e4c672010804d6249d9c7e721d067e721f067e997213732006ededededed90722399721a7321909972159c7e99721b722305721d9a721d7208907ef07220067224937214732293721e7323907e9972227221069d9c72247e7207067e720606edededed9172217222937214732493721e7325937220732693721f7327
      //
      // ErgoTreeTemplate: d820d601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d606e4c6a70505d607e4c6a70705d608e4c6a70605d609b27202730200d60ab27203730300d60bb27202730400d60cb27203730500d60db27202730600d60eb27203730700d60f8c720e01d610b27202730800d611b27203730900d6128c721101d6138c720c02d614998c720b027213d6158c720a02d616b27205730a00d6179a99a37216730bd618b27205730c00d6199d72177218d61a95919e72177218730d9a7219730e7219d61bb27205730f00d61c7e721b05d61d9d7206721cd61e998c720d028c720e02d61f998c7210028c721102d620998c7209027215d1ededededed93b272027310007204edededed93e4c672010410720593e4c672010505720693e4c672010705720793e4c6720106057208928cc77201018cc7a70193c27201c2a7ededed938c7209018c720a01938c720b018c720c01938c720d01720f938c721001721293b172027311959172147312d802d6219c721499721ca273137e721a05d622b2a5731400ededed929a997206721572089c7e999590721a721b721a9a721b7315731605721d937214f0721e937221f0721fedededed93cbc272227317938602720f7214b2db6308722273180093860272127221b2db63087222731900e6c67222040893e4c67222050e8c720401958f7214731aededec929a997206721572089c7e999590721a721b721a9a721b731b731c05721d92a39a9a72169c721b7218b27205731d0093721ef0721492721f9591721a721b731e9c721e99721ca2731f7e721a05d802d621c17201d622c1a7959072217222d802d623e4c672010804d6249d9c7e721d067e721f067e997213732006ededededed90722399721a7321909972159c7e99721b722305721d9a721d7208907ef07220067224937214732293721e7323907e9972227221069d9c72247e7207067e720606edededed9172217222937214732493721e7325937220732693721f7327
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
      //         6.1.2. Bundle is valid;
      //    6.2. Redeem: elif if (deltaLQ < 0)
      //         6.2.1. Previous epochs are compounded;
      //         6.2.2. Redeem without limits is available.
      //    6.3. Compound: if (execBudgetRem1 < execBudgetRem0)
      //         6.3.1. Previous epoch is compounded;
      //         6.3.2. Epoch is legal to perform compounding;
      //    6.4. Increase execution budget: else
      //
      // Limitations:
      // 1. Deposit
      //    1.1. Deposit can be performed before program start;
      //    1.2. During the program Deposit can't be performed until all rewards for passed epochs are distributed;
      //    1.3. Bundle box, created after every Deposit is unique;
      // 1. Redeem
      //    1.1. During the program Redeem can't be performed until all rewards for passed epochs are distributed;
      //    1.2. Redeem can be performed with no any program's logic limits after the program end;
      //    1.3. Redeem can be only performed by User, who owns unique 0x7fffffffffffffffL - 1L bundleKeyId tokens;
      // 1. Compound
      //    1.1. Reward distribution can be performed in batches;
      //    1.2. Rewards will be send on the address stored in Bundle's R4;
      //    1.3. Reward distribution should be done sequentially;
      //    1.4. All epoch allocated rewards should be fully distributed;
      //    1.5. Program budget can't be redeemed.
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
      val execBudget0       = SELF.R7[Long].get

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
      val execBudget1       = successor.R7[Long].get

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
        (execBudget1 == execBudget0) &&
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
          val curEpochToCalc  = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
          // 6.1.1.
          val prevEpochsCompoundedForDeposit =
            (programBudget0 - reservesX) + maxRoundingError0 >= (curEpochToCalc - 1) * epochAlloc

          val bundleOut = OUTPUTS(2)
          // 6.1.2.
          val validBundle =
            blake2b256(bundleOut.propositionBytes) == BundleScriptHash &&
            (poolVLQ0._1, releasedVLQ) == bundleOut.tokens(0) &&
            (poolTMP0._1, releasedTMP) == bundleOut.tokens(1) &&
            bundleOut.R4[SigmaProp].isDefined &&
            bundleOut.R5[Coll[Byte]].get == poolNFT0._1

          prevEpochsCompoundedForDeposit &&
          deltaLQ == -deltaVLQ &&
          releasedTMP == -deltaTMP &&
          validBundle

        } else if (deltaLQ < 0) { // redeem
          // 6.2.
          val releasedLQ = deltaVLQ
          val minReturnedTMP = {
            if (curEpochIx > epochNum) 0L
            else {
              val epochsDeallocated = epochNum - max(0L, curEpochIx)
              releasedLQ * epochsDeallocated
            }
          }
          val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
          // 6.2.1.
          val prevEpochsCompoundedForRedeem =
            (programBudget0 - reservesX) + maxRoundingError0 >= (curEpochToCalc - 1) * epochAlloc
          // 6.2.2.
          val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

          (prevEpochsCompoundedForRedeem || redeemNoLimit) &&
          (deltaVLQ == -deltaLQ) &&
          (deltaTMP >= minReturnedTMP)

        } else {
          val execBudgetRem0 = SELF.value
          val execBudgetRem1 = successor.value
          if (execBudgetRem1 <= execBudgetRem0) { // compound
            // 6.3.
            val epoch            = successor.R8[Int].get
            val epochsToCompound = epochNum - epoch
            // 6.3.1.
            val legalEpoch = epoch <= curEpochIx - 1
            // 6.3.2.
            val prevEpochCompounded = reservesX - epochsToCompound * epochAlloc <= epochAlloc + maxRoundingError0

            val reward  = epochAlloc.toBigInt * deltaTMP / (reservesLQ - 1L)
            val execFee = reward.toBigInt * execBudget0 / programBudget0

            legalEpoch &&
            prevEpochCompounded &&
            (-deltaX <= reward) &&
            (deltaLQ == 0L) &&
            (deltaVLQ == 0L) &&
            (execBudgetRem0 - execBudgetRem1) <= execFee // valid exec fee

          } else { // increase execution budget
            // 6.4.
            (execBudgetRem1 > execBudgetRem0) &&
            (deltaLQ == 0L) &&
            (deltaVLQ == 0L) &&
            (deltaX == 0L) &&
            (deltaTMP == 0L)
          }
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
