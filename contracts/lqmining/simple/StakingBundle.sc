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
  // ErgoTree: 19ed042004000400040404040400040804020601010400050004020404040204000400040004020502040405020402040805feffffffffffffffff01050205000404040004060404040205fcffffffffffffffff010100d80ed601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70408d609db6308a7d60ab27209730400d60bb27205730500d60cb27205730600d60d7307d60e8c720b02d1ed938cb27202730800017203959372077309d80ed60fb2a5e4e3000400d610b2e4c672040410730a00d611c672010804d61299721095e67211e47211e4c672010704d613b2a5e4e3010400d6148cb27209730b0001d615db63087213d616b27209730c00d6178c721601d6188c721602d6197e721205d61a9972189c72198c720a02d61bb27215730d00d61cb2db6308720f730e00eded93c2720fd0720895917212730fd801d61db27215731000edededededed93e4c672130408720893e4c67213050e720393c27213c2a793860272147311b27215731200938c721d017217939972188c721d02721a93721b720aedededededed93e4c672130408720893e4c67213050e720393c27213c2a793860272147313b272157314009372178c720b01937218721a93721b720aed938c721c018c720c01927e8c721c020699999d9c99997e8c720c02069d9c7ee4c672040505067e7212067e721006720d7e998cb2720273150002720e067e99997316720e9c9972067317721906720d720d958f7207731893b2db6308b2a4731900731a0086029593b17209731b8cb27209731c00018cb27209731d0001731e731f
  //
  // ErgoTreeTemplate: d80ed601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a70408d609db6308a7d60ab27209730400d60bb27205730500d60cb27205730600d60d7307d60e8c720b02d1ed938cb27202730800017203959372077309d80ed60fb2a5e4e3000400d610b2e4c672040410730a00d611c672010804d61299721095e67211e47211e4c672010704d613b2a5e4e3010400d6148cb27209730b0001d615db63087213d616b27209730c00d6178c721601d6188c721602d6197e721205d61a9972189c72198c720a02d61bb27215730d00d61cb2db6308720f730e00eded93c2720fd0720895917212730fd801d61db27215731000edededededed93e4c672130408720893e4c67213050e720393c27213c2a793860272147311b27215731200938c721d017217939972188c721d02721a93721b720aedededededed93e4c672130408720893e4c67213050e720393c27213c2a793860272147313b272157314009372178c720b01937218721a93721b720aed938c721c018c720c01927e8c721c020699999d9c99997e8c720c02069d9c7ee4c672040505067e7212067e721006720d7e998cb2720273150002720e067e99997316720e9c9972067317721906720d720d958f7207731893b2db6308b2a4731900731a0086029593b17209731b8cb27209731c00018cb27209731d0001731e731f
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
      val allocRem = poolReservesX0 - programBudget.toBigInt * epochsToCompound / epochNum - 1L
      val reward   = allocRem * deltaTMP / actualTMP - 1L

      // ===== Validating conditions ===== //
      // 2.1.1.
      val validRedeemer = redeemer.propositionBytes == redeemerProp0.propBytes
      // 2.1.2.
      val validSuccessor = if (epochsToCompound > 0) {
        val bundleTMP1 = successor.tokens(1)

        (successor.R4[SigmaProp].get == redeemerProp0) &&
        (successor.R5[Coll[Byte]].get == poolId0) &&
        (successor.propositionBytes == SELF.propositionBytes) &&
        (bundleKey0, 1L) == successor.tokens(2) &&
        (bundleTMP1._1 == bundleTMP0._1) &&
        (bundleTMP - bundleTMP1._2 == releasedTMP) &&
        (bundleVLQ1 == bundleVLQ0)
      } else {
        (successor.R4[SigmaProp].get == redeemerProp0) &&
        (successor.R5[Coll[Byte]].get == poolId0) &&
        (successor.propositionBytes == SELF.propositionBytes) &&
        (bundleKey0, 1L) == successor.tokens(1) &&
        (bundleTMP0._1 == pool0.tokens(4)._1) &&
        (bundleTMP == releasedTMP) &&
        (bundleVLQ1 == bundleVLQ0)
      }
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
