{
  val expectedLQ       = SELF.R4[Coll[Byte]].get
  val expectedLQAmount = SELF.R5[Long].get
  val redeemerPk       = SELF.R6[SigmaProp].get

  val redeemerOut = OUTPUTS(1)

  val validRedeemerOut =
    redeemerOut.propositionBytes == redeemerPk.propBytes &&
    (expectedLQ, expectedLQAmount) == redeemerOut.tokens(0)

  redeemerPk || validRedeemerOut
}
