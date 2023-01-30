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
  // ErgoTree: 198f041d04000400040404040400040206010104080400050004020402040204000404050204040400040805feffffffffffffffff01050205000404040004060404040205fcffffffffffffffff010100d80dd601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70408d609db6308a7d60ab27209730400d60bb27205730500d60c7306d60d8cb2720573070002d1ed938cb27202730800017203959372077309d80bd60eb2a5e4e3000400d60fb2a5e4e3010400d610db6308720fd611b27210730a00d612b27209730b00d6138c721202d614b2e4c672040410730c00d615c672010804d61699721495e67215e47215e4c672010704d6177e721605d618b2db6308720e730d00eded93c2720ed07208edededededed93e4c6720f0408720893e4c6720f050e720393c2720fc2a79386028cb27209730e0001730fb27210731000938c7211018c721201939972138c7211029972139c72178c720a0293b27210731100720aed938c7218018c720b01927e8c7218020699999d9c99997e8c720b02069d9c7ee4c672040505067e7216067e721406720c7e998cb2720273120002720d067e99997313720d9c9972067314721706720c720c958f7207731593b2db6308b2a473160073170086029593b1720973188cb27209731900018cb27209731a0001731b731c
  //
  // ErgoTreeTemplate: d80dd601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70408d609db6308a7d60ab27209730400d60bb27205730500d60c7306d60d8cb2720573070002d1ed938cb27202730800017203959372077309d80bd60eb2a5e4e3000400d60fb2a5e4e3010400d610db6308720fd611b27210730a00d612b27209730b00d6138c721202d614b2e4c672040410730c00d615c672010804d61699721495e67215e47215e4c672010704d6177e721605d618b2db6308720e730d00eded93c2720ed07208edededededed93e4c6720f0408720893e4c6720f050e720393c2720fc2a79386028cb27209730e0001730fb27210731000938c7211018c721201939972138c7211029972139c72178c720a0293b27210731100720aed938c7218018c720b01927e8c7218020699999d9c99997e8c720b02069d9c7ee4c672040505067e7216067e721406720c7e998cb2720273120002720d067e99997313720d9c9972067314721706720c720c958f7207731593b2db6308b2a473160073170086029593b1720973188cb27209731900018cb27209731a0001731b731c
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
      val bundleTMP1          = successor.tokens(1)
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
      val allocRem = poolReservesX0 - programBudget.toBigInt * epochsToCompound / epochNum - 1L
      val reward   = allocRem * deltaTMP / actualTMP - 1L

      // ===== Validating conditions ===== //
      // 2.1.1.
      val validRedeemer = redeemer.propositionBytes == redeemerProp0.propBytes
      // 2.1.2.
      val validSuccessor =
        (successor.R4[SigmaProp].get == redeemerProp0) &&
        (successor.R5[Coll[Byte]].get == poolId0) &&
        (successor.propositionBytes == SELF.propositionBytes) &&
        (bundleKey0, 1L) == successor.tokens(2) &&
        (bundleTMP1._1 == bundleTMP0._1) &&
        (bundleTMP - bundleTMP1._2 == releasedTMP) &&
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
