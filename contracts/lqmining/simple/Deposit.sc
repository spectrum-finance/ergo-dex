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
  // Validations:
  // 1. Assets are deposited into the correct LM Pool;
  // 2. Redeemer PubKey matches and correct Bundle identification token is received;
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
    redeemerOut.propositionBytes == RedeemerPk.propBytes &&
      (bundleKeyId, 0x7fffffffffffffffL) == redeemerOut.tokens(0)
  // 3.
  val validBundle =
    bundleOut.R4[SigmaProp].get.propBytes == RedeemerPk.propBytes &&
      bundleOut.R5[Coll[Byte]].get == bundleKeyId &&
      (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(1) &&
      (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(2)

  sigmaProp(RedeemerPk || (validPoolIn && validRedeemerOut && validBundle))
}
