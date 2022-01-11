{
  val deadline = SELF.R4[Int].get
  val Pk       = SELF.R5[SigmaProp].get

  val maybeSuccessor     = OUTPUTS(0)
  val isTransferOrRelock = maybeSuccessor.propositionBytes == SELF.propositionBytes

  val validAction =
    if (isTransferOrRelock) {
      val lockedAsset = SELF.tokens(0)
      val movedAsset  = maybeSuccessor.tokens(0)

      maybeSuccessor.R4[Int].get >= deadline &&
      movedAsset._1 == lockedAsset._1 &&
      movedAsset._2 >= lockedAsset._2
    } else
      deadline < HEIGHT

  sigmaProp(Pk && validAction)
}
