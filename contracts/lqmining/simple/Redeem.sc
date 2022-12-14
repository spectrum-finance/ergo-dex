{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the LM Pool.
  // Registers:
  //   R4[Coll[Byte]]: LM Pool ID (tokenId) // used to authenticate pool.
  //
  // Tokens:
  //   0:
  //     _1: BundleKeyId
  //     _2: 0x7fffffffffffffffL - 1L
  //
  // Constants:
  // {1}  -> RefundPk[ProveDlog]
  // {2}  -> RedeemerProp[Coll[Byte]]
  // {3}  -> ExpectedLQ[Coll[Byte]]
  // {4}  -> ExpectedLQAmount[Long]
  //
  // ErgoTree:         19a70206040208cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2
  //                   adddb15a0e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
  //                   aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
  //                   aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
  //                   0e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
  //                   bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
  //                   bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb05fe887a
  //                   0400d801d601b2a5730000eb027301d1ed93c27201730293860273037304b2db6308720173
  //                   0500
  //
  // ErgoTreeTemplate: d801d601b2a5730000eb027301d1ed93c27201730293860273037304b2db63087201730500
  //
  // Validations:
  // 1. Redeemer out is valid: Redeemer PubKey matches PubKey in Bundle Box; vLQ token amount; Bundle Key token amount.
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
