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
  // {2}  -> RedeemerProp[Coll[Byte]]
  // {5}  -> RefundPk[ProveDlog]
  // {7}  -> PoolId[Coll[Byte]]
  // {13} -> ExpectedNumEpochs[Int]
  //
  // ErgoTreeTemplate: d807d601b2a4730000d602db63087201d603b2a5730100d6047302d605c57201d606b2a5730300
  //                   d6078cb2db6308a773040002eb027305d1eded938cb27202730600017307ed93c2720372049386
  //                   0272057308b2db63087203730900ededed93e4c67206040e720493e4c67206050e72059386028c
  //                   b27202730a00017207b2db63087206730b009386028cb27202730c00019c7207730db2db630872
  //                   06730e00
  //
  // Validations:
  // 1. Assets are deposited into the correct LM Pool;
  // 2. Redeemer PubKey matches and correct Bundle Key token is received;
  // 3. Bundle stores correct: Redeemer PubKey; bundleKeyId; vLQ token ID; vLQ token amount; TMP token ID; TMP token amount.
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
  val expectedTMP = expectedVLQ * ExpectedNumEpochs

  // ===== Validating conditions ===== //
  // 1.
  val validPoolIn = poolIn.tokens(0)._1 == PoolId
  // 2.
  val validRedeemerOut =
    redeemerOut.propositionBytes == RedeemerProp &&
      (bundleKeyId, 0x7fffffffffffffffL) == redeemerOut.tokens(0)
  // 3.
  val validBundle =
    bundleOut.R4[Coll[Byte]].get == RedeemerProp &&
      bundleOut.R5[Coll[Byte]].get == bundleKeyId &&
      (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(0) &&
      (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(1)

  sigmaProp(RefundPk || (validPoolIn && validRedeemerOut && validBundle))
}
