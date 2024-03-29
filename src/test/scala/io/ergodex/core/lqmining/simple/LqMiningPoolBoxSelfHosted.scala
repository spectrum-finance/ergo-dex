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
  override val validatorBytes: String,
  override val constants: Map[Int, Any] = Map(23 -> blake2b256("staking_bundle".getBytes().toVector))
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
      //   R6[Long]: Max Rounding Error // Tokens rounding delta max value.
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
      // Constants:
      // {23}  -> BundleScriptHash[Coll[Byte]]
      //
      // ErgoTree: 19c0062804000400040204020404040404060406040804080404040204000400040204020400040a050005000404040204020e200508f3623d4b2be3bdb9737b3e65644f011167eefb830d9965205f022ceda40d0400040205000402040204060500050005feffffffffffffffff01050005000402060101050005000100d81fd601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d606e4c6a70505d607e4c6a70605d608b27202730200d609b27203730300d60ab27202730400d60bb27203730500d60cb27202730600d60db27203730700d60e8c720d01d60fb27202730800d610b27203730900d6118c721001d6128c720b02d613998c720a027212d6148c720902d615b27205730a00d6169a99a37215730bd617b27205730c00d6189d72167217d61995919e72167217730d9a7218730e7218d61ab27205730f00d61b7e721a05d61c9d7206721bd61d998c720c028c720d02d61e8c721002d61f998c720f02721ed1ededededed93b272027310007204ededed93e4c672010410720593e4c672010505720693e4c6720106057207928cc77201018cc7a70193c27201c2a7ededed938c7208018c720901938c720a018c720b01938c720c01720e938c720f01721193b172027311959172137312d802d6209c721399721ba273137e721905d621b2a5731400ededed929a7e9972067214067e7207067e9c7e9995907219721a72199a721a7315731605721c06937213f0721d937220f0721fedededed93cbc272217317938602720e7213b2db6308722173180093860272117220b2db63087221731900e6c67221060893e4c67221070e8c720401958f7213731aededec929a7e9972067214067e7207067e9c7e9995907219721a72199a721a731b731c05721c0692a39a9a72159c721a7217b27205731d0093721df0721392721f95917219721a731e9c721d99721ba2731f7e721905d804d620e4c672010704d62199721a7220d6227e722105d62399997320721e9c7212722295ed917223732191721f7322edededed9072209972197323909972149c7222721c9a721c7207907ef0998c7208027214069d9c99997e7214069d9c7e7206067e7221067e721a0673247e721f067e722306937213732593721d73267327
      //
      // ErgoTreeTemplate: d81fd601b2a5730000d602db63087201d603db6308a7d604b27203730100d605e4c6a70410d606e4c6a70505d607e4c6a70605d608b27202730200d609b27203730300d60ab27202730400d60bb27203730500d60cb27202730600d60db27203730700d60e8c720d01d60fb27202730800d610b27203730900d6118c721001d6128c720b02d613998c720a027212d6148c720902d615b27205730a00d6169a99a37215730bd617b27205730c00d6189d72167217d61995919e72167217730d9a7218730e7218d61ab27205730f00d61b7e721a05d61c9d7206721bd61d998c720c028c720d02d61e8c721002d61f998c720f02721ed1ededededed93b272027310007204ededed93e4c672010410720593e4c672010505720693e4c6720106057207928cc77201018cc7a70193c27201c2a7ededed938c7208018c720901938c720a018c720b01938c720c01720e938c720f01721193b172027311959172137312d802d6209c721399721ba273137e721905d621b2a5731400ededed929a7e9972067214067e7207067e9c7e9995907219721a72199a721a7315731605721c06937213f0721d937220f0721fedededed93cbc272217317938602720e7213b2db6308722173180093860272117220b2db63087221731900e6c67221060893e4c67221070e8c720401958f7213731aededec929a7e9972067214067e7207067e9c7e9995907219721a72199a721a731b731c05721c0692a39a9a72159c721a7217b27205731d0093721df0721392721f95917219721a731e9c721d99721ba2731f7e721905d804d620e4c672010704d62199721a7220d6227e722105d62399997320721e9c7212722295ed917223732191721f7322edededed9072209972197323909972149c7222721c9a721c7207907ef0998c7208027214069d9c99997e7214069d9c7e7206067e7221067e721a0673247e721f067e722306937213732593721d73267327
      //
      // ErgoTreeTemplateHash: 728bc5b8f6244f191bd5c6b783b7895981dc37f1504458d0fc8e02754ecb3eff
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
      //    6.3. Compound: else
      //         6.3.1. Previous epoch is compounded;
      //         6.3.2. Epoch is legal to perform compounding;
      //
      // Limitations:
      // 1. Deposit
      //    1.1. Deposit can be performed before program start;
      //    1.2. During the program Deposit can't be performed until all rewards for passed epochs are distributed;
      //    1.3. Bundle box created after every Deposit is unique;
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
          val curEpochToCalc  = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
          // 6.1.1.
          val prevEpochsCompoundedForDeposit =
            ((programBudget0 - reservesX).toBigInt + maxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc

          val bundleOut = OUTPUTS(2)
          // 6.1.2.
          val validBundle =
            blake2b256(bundleOut.propositionBytes) == BundleScriptHash &&
            (poolVLQ0._1, releasedVLQ) == bundleOut.tokens(0) &&
            (poolTMP0._1, releasedTMP) == bundleOut.tokens(1) &&
            bundleOut.R6[SigmaProp].isDefined &&
            bundleOut.R7[Coll[Byte]].get == poolNFT0._1

          prevEpochsCompoundedForDeposit &&
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
          val curEpochToCalc = if (curEpochIx <= epochNum) curEpochIx else epochNum + 1
          // 6.2.1.
          val prevEpochsCompoundedForRedeem =
            ((programBudget0 - reservesX).toBigInt + maxRoundingError0) >= (curEpochToCalc - 1) * epochAlloc
          // 6.2.2.
          val redeemNoLimit = HEIGHT >= programStart + epochNum * epochLen + redeemLimitDelta

          (prevEpochsCompoundedForRedeem || redeemNoLimit) &&
          (deltaVLQ == -deltaLQ) &&
          (deltaTMP >= minReturnedTMP)

        } else { // compound
          // 6.3.
          val epoch            = successor.R7[Int].get
          val epochsToCompound = epochNum - epoch
          // 6.3.1.
          val legalEpoch = epoch <= curEpochIx - 1
          // 6.3.2.
          val prevEpochCompounded = reservesX - epochsToCompound * epochAlloc <= epochAlloc + maxRoundingError0

          val actualTMP  = 0x7fffffffffffffffL - poolTMP0._2 - reservesLQ * epochsToCompound
          val allocRem = reservesX - programBudget0.toBigInt * epochsToCompound / epochNum - 1L

          if (actualTMP > 0 && deltaTMP > 0) {
            val reward = allocRem * deltaTMP / actualTMP

            legalEpoch &&
            prevEpochCompounded &&
            (-deltaX <= reward) &&
            (deltaLQ == 0L) &&
            (deltaVLQ == 0L)

          } else { false }

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
