{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the LM Pool.
  //
  // Validations:
  // 1. Redeemer out is valid: LQ token ID; LQ token amount.

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
