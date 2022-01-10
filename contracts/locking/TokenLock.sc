{
  val deadline = SELF.R4[Int].get
  val Pk       = SELF.R5[SigmaProp].get

  val maybeSuccessor     = OUTPUTS(0)
  val isTransferOrRelock = maybeSuccessor.propositionBytes == SELF.propositionBytes

  val validAction =
    if (isTransferOrRelock)
      maybeSuccessor.R4[Int].get >= deadline
    else
      deadline < HEIGHT

  sigmaProp(Pk && validAction)
}
