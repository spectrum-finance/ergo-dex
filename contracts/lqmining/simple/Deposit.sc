{
  val redeemerOut = OUTPUTS(1) // 0 -> pool_out, 1 -> redeemer_out, 2 -> bundle_out
  val bundleOut   = OUTPUTS(2)

  val poolIn      = INPUTS(0)
  val bundleKeyId = poolIn.id

  val deposit     = SELF.tokens(0)
  val expectedVLQ = deposit._2
  val expectedTMP = expectedVLQ * ExpectedNumEpochs

  val validPoolIn = poolIn.tokens(0)._1 == PoolId
  val validRedeemerOut =
    redeemerOut.propositionBytes == RedeemerPk.propBytes &&
    (bundleKeyId, 0x7fffffffffffffffL) == redeemerOut.tokens(0)

  val validBundle =
    bundleOut.R4[SigmaProp].get.propBytes == RedeemerPk.propBytes &&
    bundleOut.R5[Coll[Byte]].get == bundleKeyId &&
    (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(1) &&
    (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(2)

  RedeemerPk || (validPoolIn && validRedeemerOut && validBundle)
}
