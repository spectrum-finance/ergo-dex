{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the LM Pool.
  // Tokens:
  //   0:
  //     _1: BundleKeyId
  //     _2: 0x7fffffffffffffffL
  // Validations:
  // 1. Redeemer out is valid: Redeemer PubKey matches PubKey in Bundle Box; vLQ token ID; vLQ token amount; bundle key ID.
  //
  // ===== Getting OUTPUTS data ===== //
  val redeemerOut = OUTPUTS(1)

  // ===== Validating conditions ===== //
  // 1.
  val validRedeemerOut = {
    (redeemerOut.propositionBytes == RedeemerPk.propBytes) &&
      ((ExpectedLQ, ExpectedLQAmount) == redeemerOut.tokens(0))
  }
  sigmaProp(RedeemerPk || validRedeemerOut)
}
