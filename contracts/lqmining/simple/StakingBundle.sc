{ // ===== Contract Information ===== //
  // Name: StakingBundle
  // Description: Contract that validates a compounding in the LM pool.
  //
  // ===== Bundle Box ===== //
  // Registers:
  //   R4[SigmaProp]: Redeemer Sigma Proposition  // where the reward should be sent.
  //   R5[Coll[Byte]]: LM Pool ID (tokenId) // used to authenticate pool.
  //
  // Tokens:
  //   0:
  //     _1: vLQ Token ID  // tokens representing locked share of LQ.
  //     _2: Amount of vLQ tokens
  //   1:
  //     _1: TMP Token ID  // left program epochs times liquidity.
  //     _2: Amount of the TMP tokens
  //   2:
  //     _1: BundleKeyId
  //     _2: 1L
  //
  // ContextExtension constants:
  // 0: Int - redeemer output index;
  // 1: Int - successor output index;
  // * indexes are dynamic to allow batch compounding.
  //
  // ErgoTree: 19bc042004000400040404040400040804020601010400050004020404040204080400040004020502040405020402040005feffffffffffffffff01050205000404040004060404040205fcffffffffffffffff010100d80dd601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70408d609db6308a7d60ab27209730400d60b8cb2720573050002d60cb27205730600d60d7307d1ed938cb27202730800017203959372077309d80dd60eb2a5e4e3000400d60fb2a5e4e3010400d610b2e4c672040410730a00d611c672010804d61299721095e67211e47211e4c672010704d6138cb27209730b0001d614db6308720fd615b27209730c00d6168c721502d6177e721205d6189972169c72178c720a02d619998cb27202730d0002720bd61ab2db6308720e730e00eded93c2720ed07208ededededed93e4c6720f0408720893e4c6720f050e720393c2720fc2a795917212730fd801d61bb27214731000eded93860272137311b27214731200938c721b018c721501939972168c721b02721893860272137313b27214731400937218721993b27214731500720aed938c721a018c720c01927e8c721a020699999d9c99997e8c720c02069d9c7ee4c672040505067e7212067e721006720d7e7219067e99997316720b9c9972067317721706720d720d958f7207731893b2db6308b2a4731900731a0086029593b17209731b8cb27209731c00018cb27209731d0001731e731f
  //
  // ErgoTreeTemplate: d80dd601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70408d609db6308a7d60ab27209730400d60b8cb2720573050002d60cb27205730600d60d7307d1ed938cb27202730800017203959372077309d80dd60eb2a5e4e3000400d60fb2a5e4e3010400d610b2e4c672040410730a00d611c672010804d61299721095e67211e47211e4c672010704d6138cb27209730b0001d614db6308720fd615b27209730c00d6168c721502d6177e721205d6189972169c72178c720a02d619998cb27202730d0002720bd61ab2db6308720e730e00eded93c2720ed07208ededededed93e4c6720f0408720893e4c6720f050e720393c2720fc2a795917212730fd801d61bb27214731000eded93860272137311b27214731200938c721b018c721501939972168c721b02721893860272137313b27214731400937218721993b27214731500720aed938c721a018c720c01927e8c721a020699999d9c99997e8c720c02069d9c7ee4c672040505067e7212067e721006720d7e7219067e99997316720b9c9972067317721706720d720d958f7207731893b2db6308b2a4731900731a0086029593b17209731b8cb27209731c00018cb27209731d0001731e731f
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

  val redeemerProp0 = SELF.R4[SigmaProp].get
  val poolId0       = SELF.R5[Coll[Byte]].get

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
      val redeemer         = OUTPUTS(redeemerOutIx)
      val successor        = OUTPUTS(successorIndex)
      val poolReservesTMP1 = pool1.tokens(4)._2

      val bundleVLQ1          = successor.tokens(0)
      val redeemerRewardToken = redeemer.tokens(0)
      val epoch_              = pool1.R8[Int]
      val epoch               = if (epoch_.isDefined) epoch_.get else pool1.R7[Int].get

      // ===== Getting deltas and calculate reward ===== //
      val epochsToCompound = epochNum - epoch
      val bundleVLQ        = bundleVLQ0._2
      val bundleTMP        = bundleTMP0._2
      val releasedTMP      = bundleTMP0._2 - epochsToCompound * bundleVLQ
      val deltaTMP         = poolReservesTMP1 - poolReservesTMP0

      val actualTMP = 0x7fffffffffffffffL - poolReservesTMP0 - (poolReservesLQ0 - 1L) * epochsToCompound
      val allocRem  = poolReservesX0 - programBudget.toBigInt * epochsToCompound / epochNum - 1L
      val reward    = allocRem * deltaTMP / actualTMP - 1L

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
        (successor.R4[SigmaProp].get == redeemerProp0) &&
        (successor.R5[Coll[Byte]].get == poolId0) &&
        (successor.propositionBytes == SELF.propositionBytes) &&
        validTMPAndKey &&
        (releasedTMP == deltaTMP) &&
        (bundleVLQ1 == bundleVLQ0)
      // 2.1.3.
      val validReward =
        (redeemerRewardToken._1 == pool0.tokens(1)._1) &&
        (redeemerRewardToken._2 >= reward - 1L)

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
