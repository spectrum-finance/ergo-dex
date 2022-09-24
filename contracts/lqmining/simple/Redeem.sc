{
  val redeemerOut = OUTPUTS(1)

  val validRedeemerOut =
    redeemerOut.propositionBytes == RedeemerPk.propBytes &&
    (ExpectedLQ, ExpectedLQAmount) == redeemerOut.tokens(0)

  RedeemerPk || validRedeemerOut
}
