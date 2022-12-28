{ // ===== Contract Information ===== //
  // Name: Deposit
  // Description: Contract that validates user's deposit into the LM Pool.
  //
  // ===== Deposit Box ===== //
  // Tokens:
  //   0:
  //     _1: LQ Token ID  // identifier for the stake pool box.
  //     _2: Amount of LQ Tokens to deposit
  //
  // Constants:
  // {1}  -> PoolId[Coll[Byte]]
  // {3}  -> RedeemerProp[Coll[Byte]]
  // {6}  -> RefundPk[ProveDlog]
  // {13} -> ExpectedNumEpochs[Int]
  //
  // ErgoTree: 198e031104000e20000000000000000000000000000000000000000000000000000000000000000004020e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0404040008cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a040005fcffffffffffffffff01040004060400040804f00d040205020404d808d601b2a4730000d602db63087201d6037301d604b2a5730200d6057303d606c57201d607b2a5730400d6088cb2db6308a773050002eb027306d1eded938cb27202730700017203ed93c27204720593860272067308b2db63087204730900edededed93e4c67207040e720593e4c67207050e72039386028cb27202730a00017208b2db63087207730b009386028cb27202730c00019c72087e730d05b2db63087207730e009386027206730fb2db63087207731000
  //
  // ErgoTreeTemplate: d808d601b2a4730000d602db63087201d6037301d604b2a5730200d6057303d606c57201d607b2a5730400d6088cb2db6308a773050002eb027306d1eded938cb27202730700017203ed93c27204720593860272067308b2db63087204730900edededed93e4c67207040e720593e4c67207050e72039386028cb27202730a00017208b2db63087207730b009386028cb27202730c00019c72087e730d05b2db63087207730e009386027206730fb2db63087207731000
  //
  // Validations:
  // 1. Assets are deposited into the correct LM Pool;
  // 2. Redeemer PubKey matches and correct Bundle Key token amount token is received;
  // 3. Bundle stores correct: Redeemer PubKey; vLQ token amount; TMP token amount; Bundle Key token amount.
  //
  // ===== Getting SELF data ===== //
  val deposit = SELF.tokens(0)

  // ===== Getting INPUTS data ===== //
  val poolIn = INPUTS(0)
  val bundleKeyId = poolIn.id

  // ===== Getting OUTPUTS data ===== //
  val redeemerOut = OUTPUTS(1) // 0 -> pool_out, 1 -> redeemer_out, 2 -> bundle_out
  val bundleOut = OUTPUTS(2)

  // ===== Calculations ===== //
  val expectedVLQ = deposit._2
  val expectedNumEpochs = ExpectedNumEpochs
  val expectedTMP = expectedVLQ * expectedNumEpochs

  // ===== Validating conditions ===== //
  // 1.
  val validPoolIn = poolIn.tokens(0)._1 == PoolId
  // 2.
  val validRedeemerOut =
    redeemerOut.propositionBytes == RedeemerProp &&
      (bundleKeyId, 0x7fffffffffffffffL - 1L) == redeemerOut.tokens(0)
  // 3.
  val validBundle =
    bundleOut.R4[Coll[Byte]].get == RedeemerProp &&
      bundleOut.R5[Coll[Byte]].get == PoolId &&
      (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(0) &&
      (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(1) &&
      (bundleKeyId, 1L) == bundleOut.tokens(2)

  sigmaProp(RefundPk || (validPoolIn && validRedeemerOut && validBundle))
}
