{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the LM Pool.
  // Tokens:
  //   0:
  //     _1: BundleKeyId
  //     _2: 0x7fffffffffffffffL
  //
  // Constants:
  // {1}  -> RefundPk[ProveDlog]
  // {2}  -> RedeemerProp[Coll[Byte]]
  // {3}  -> ExpectedLQ[Coll[Byte]]
  // {4}  -> ExpectedLQAmount[Long]
  //
  // ErgoTreeTemplate: d801d601b2a5730000eb027301d1ed93c27201730293860273037304b2db63087201730500
  //
  // Validations:
  // 1. Redeemer out is valid: Redeemer PubKey matches PubKey in Bundle Box; vLQ token ID; vLQ token amount; bundle key ID.
  //
  // ===== Getting OUTPUTS data ===== //
  val redeemerOut = OUTPUTS(1)

  // ===== Validating conditions ===== //
  // 1.
  val validRedeemerOut = {
    (redeemerOut.propositionBytes == RedeemerProp) &&
      ((ExpectedLQ, ExpectedLQAmount) == redeemerOut.tokens(0))
  }
  sigmaProp(RefundPk || validRedeemerOut)
}
