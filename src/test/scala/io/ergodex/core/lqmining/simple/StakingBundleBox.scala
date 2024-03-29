package io.ergodex.core.lqmining.simple

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
  override val validatorBytes: String = hex("staking_bundle")
) extends BoxSim[F] {

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // ===== Contract Information ===== //
      // Name: StakingBundle
      // Description: Contract that validates a compounding in the LM pool.
      //
      // ===== Bundle Box ===== //
      // Registers:
      //   R4[Coll[Byte]]: BundleKeyToken name.
      //   R5[Coll[Byte]]: BundleKeyToken info.
      //   R6[SigmaProp]: Redeemer Sigma Proposition  // where the reward should be sent.
      //   R7[Coll[Byte]]: LM Pool ID (tokenId) // used to authenticate pool.
      //
      // Tokens:
      //   0:
      //     _1: vLQ Token ID  // tokens representing locked share of LQ.
      //     _2: Amount of vLQ tokens
      //   1:
      //     _1: TMP Token ID  // left program epochs times liquidity.
      //     _2: Amount of the TMP tokens
      //   2:
      //     _1: BundleKeyId // BundleKeyToken.
      //     _2: 1L
      //
      // ContextExtension constants:
      // 0: Int - redeemer output index;
      // 1: Int - successor output index;
      // * indexes are dynamic to allow batch compounding.
      //
      // ErgoTree: 19bc04210400040004040404040004020601010601000400050004020404040205feffffffffffffffff010408040004020502040405020402040004000101010005000404040004060404040205fcffffffffffffffff010100d80dd601b2a5730000d602db63087201d603e4c6a7070ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70608d609db6308a7d60ab27209730400d60bb27205730500d60c7306d60d7307d1ed938cb27202730800017203959372077309d80cd60eb2a5e4e3000400d60fb2a5e4e3010400d610b2e4c672040410730a00d611c672010804d61299721095e67211e47211e4c672010704d6138cb27209730b0001d614db6308720fd615b27209730c00d6168c721502d6177e721205d6189972169c72178c720a02d6199d9c99997e8c720b02069d9c7ee4c672040505067e7212067e721006720c7e7218067e9999730d8cb27205730e00029c7206721706eded93c2720ed07208edededed93e4c6720f0608720893e4c6720f070e720393c2720fc2a795917212730fd801d61ab27214731000eded93860272137311b27214731200938c721a018c721501939972168c721a02721893860272137313b2721473140093b27214731500720a95917219720dd801d61ab2db6308720e731600ed938c721a018c720b01927e8c721a0206997219720c95937219720d73177318958f7207731993b2db6308b2a4731a00731b0086029593b17209731c8cb27209731d00018cb27209731e0001731f7320
      //
      // ErgoTreeHash: 0508f3623d4b2be3bdb9737b3e65644f011167eefb830d9965205f022ceda40d
      //
      // ErgoTreeTemplate: d80dd601b2a5730000d602db63087201d603e4c6a7070ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70608d609db6308a7d60ab27209730400d60bb27205730500d60c7306d60d7307d1ed938cb27202730800017203959372077309d80cd60eb2a5e4e3000400d60fb2a5e4e3010400d610b2e4c672040410730a00d611c672010804d61299721095e67211e47211e4c672010704d6138cb27209730b0001d614db6308720fd615b27209730c00d6168c721502d6177e721205d6189972169c72178c720a02d6199d9c99997e8c720b02069d9c7ee4c672040505067e7212067e721006720c7e7218067e9999730d8cb27205730e00029c7206721706eded93c2720ed07208edededed93e4c6720f0608720893e4c6720f070e720393c2720fc2a795917212730fd801d61ab27214731000eded93860272137311b27214731200938c721a018c721501939972168c721a02721893860272137313b2721473140093b27214731500720a95917219720dd801d61ab2db6308720e731600ed938c721a018c720b01927e8c721a0206997219720c95937219720d73177318958f7207731993b2db6308b2a4731a00731b0086029593b17209731c8cb27209731d00018cb27209731e0001731f7320
      //
      // ErgoTreeTemplateHash: 88c122183349cde8c85f70287a098db39f1b3138e5650659f3c1746d02e435cb
      //
      // Validations:
      // 1. LM Pool NFT (Token ID) is valid;
      // 2. Action is valid:
      //    2.1. Compound:
      //         2.1.1 Valid redeemer;
      //         2.1.2 Valid successor;
      //         2.1.3 Valid reward.
      //    2.2. Redeem:
      //         2.2.1 bundleKeyId tokens matches with RedeemBox.
      //
      // Limitations:
      // 1. Reward distribution can be performed in batches;
      // 2. Rewards will be send on the address stored in R4;
      // 3. Redeem can be performed only by input with 0x7fffffffffffffffL - 1L bundleKeyId tokens unique for every Bundle.
      //
      // ===== Getting SELF data ===== //
      val bundleVLQ0 = SELF.tokens(0)

      val redeemerProp0 = SELF.R6[SigmaProp].get
      val poolId0       = SELF.R7[Coll[Byte]].get

      // ===== Getting INPUTS data ===== //
      val pool0            = INPUTS(0)
      val poolReservesX0   = pool0.tokens(1)._2
      val poolReservesLQ0  = pool0.tokens(2)._2
      val poolReservesTMP0 = pool0.tokens(4)._2

      // ===== Getting OUTPUTS data ===== //
      val pool1   = OUTPUTS(0)
      val deltaLQ = pool1.tokens(2)._2 - poolReservesLQ0

      // ===== Validating conditions ===== //
      // 1.
      val validPool = pool1.tokens(0)._1 == poolId0
      // 2.
      val validAction =
        if (deltaLQ == 0L) { // compound
          // 2.1.
          // ===== Getting SELF data ===== //
          val bundleKey0 = SELF.tokens(2)._1
          val bundleTMP0 = SELF.tokens(1)

          // ===== Getting INPUTS data ===== //
          val conf          = pool0.R4[Coll[Int]].get
          val programBudget = pool0.R5[Long].get
          val epochNum      = conf(1)

          val redeemerOutIx  = getVar[Int](0).get
          val successorIndex = getVar[Int](1).get

          // ===== Getting OUTPUTS data ===== //
          val redeemer  = OUTPUTS(redeemerOutIx)
          val successor = OUTPUTS(successorIndex)

          val bundleVLQ1 = successor.tokens(0)
          val epoch_     = pool1.R8[Int]
          val epoch      = if (epoch_.isDefined) epoch_.get else pool1.R7[Int].get

          // ===== Getting deltas and calculate reward ===== //
          val epochsToCompound = epochNum - epoch
          val bundleVLQ        = bundleVLQ0._2
          val bundleTMP        = bundleTMP0._2
          val releasedTMP      = bundleTMP0._2 - epochsToCompound * bundleVLQ

          val actualTMP = 0x7fffffffffffffffL - poolReservesTMP0 - poolReservesLQ0 * epochsToCompound
          val allocRem  = poolReservesX0 - programBudget.toBigInt * epochsToCompound / epochNum - 1L
          val reward    = allocRem * releasedTMP / actualTMP

          // ===== Validating conditions ===== //
          // 2.1.1.
          val validRedeemer = redeemer.propositionBytes == redeemerProp0.propBytes
          // 2.1.2.
          val validTMPAndKey = if (epochsToCompound > 0) {
            val bundleTMP1 = successor.tokens(1)
            (bundleKey0, 1L) == successor.tokens(2) &&
            (bundleTMP1._1 == bundleTMP0._1) &&
            (bundleTMP - bundleTMP1._2 == releasedTMP)
          } else {
            (bundleKey0, 1L) == successor.tokens(1)
          }
          val validSuccessor =
            (successor.R6[SigmaProp].get == redeemerProp0) &&
            (successor.R7[Coll[Byte]].get == poolId0) &&
            (successor.propositionBytes == SELF.propositionBytes) &&
            validTMPAndKey &&
            (bundleVLQ1 == bundleVLQ0)
          // 2.1.3.
          val validReward = if (reward > 0) {
            val redeemerRewardToken = redeemer.tokens(0)
            (redeemerRewardToken._1 == pool0.tokens(1)._1) &&
            (redeemerRewardToken._2 >= reward - 1L)
          } else if (reward == 0)
            true
          else false

          validRedeemer &&
          validSuccessor &&
          validReward

        } else if (deltaLQ < 0L) { // redeem (validated by redeem order)
          // 2.2.
          // ===== Getting SELF data ===== //
          val bundleKey0 = {
            if (SELF.tokens.size == 3) {
              SELF.tokens(2)._1
            } else SELF.tokens(1)._1
          }

          // ===== Getting INPUTS data ===== //
          val permitIn       = INPUTS(2)
          val requiredPermit = (bundleKey0, 0x7fffffffffffffffL - 1L)

          // ===== Validating conditions ===== //
          // 2.2.1.
          permitIn.tokens(0) == requiredPermit

        } else {
          false
        }

      sigmaProp(
        validPool &&
        validAction
      )
    }
}

object StakingBundleBox {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSim[G]): StakingBundleBox[F] =
    new StakingBundleBox(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[StakingBundleBox, F] =
    AnyBox.tryFromBox.translate(apply[F, NonRunnable])
}
