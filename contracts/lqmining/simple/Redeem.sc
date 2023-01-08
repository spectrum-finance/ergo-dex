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
  //  {1} -> RefundPk[ProveDlog]
  //  {2} -> RedeemerProp[Coll[Byte]]
  //  {3} -> ExpectedLQ[Coll[Byte]]
  //  {4} -> ExpectedLQAmount[Long]
  //  {6} -> MinerPropBytes[Coll[Byte]]
  //  {9} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19c0030a040208cd03d36d7e86b0fe7d8aec204f0ae6c2be6563fc7a443d69501d73dfe9c2adddb15a0e69aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0e69bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb05fe887a04000e69cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc0500050005fe887ad801d601b2a5730000eb027301d1eded93c27201730293860273037304b2db6308720173050090b0ada5d90102639593c272027306c1720273077308d90102599a8c7202018c7202027309
  //
  // ErgoTreeTemplate: d801d601b2a5730000eb027301d1eded93c27201730293860273037304b2db6308720173050090b0ada5d90102639593c272027306c1720273077308d90102599a8c7202018c7202027309
  //
  // Validations:
  // 1. Redeemer out is valid: Redeemer PubKey matches PubKey in Bundle Box; vLQ token amount; Bundle Key token amount.
  // 4. Miner Fee
  //
  // ===== Getting OUTPUTS data ===== //
  val redeemerOut = OUTPUTS(1)

  // ===== Validating conditions ===== //
  // 1.
  val validRedeemerOut = {
    (redeemerOut.propositionBytes == RedeemerProp) &&
    ((ExpectedLQ, ExpectedLQAmount) == redeemerOut.tokens(0))
  }
  // 2.
  val validMinerFee = OUTPUTS
    .map { (o: Box) =>
      if (o.propositionBytes == MinerPropBytes) o.value else 0L
    }
    .fold(0L, (a: Long, b: Long) => a + b) <= MaxMinerFee

  sigmaProp(RefundPk || validRedeemerOut && validMinerFee)
}
