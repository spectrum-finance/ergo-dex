{
  val isMature = SELF.creationInfo._1 + LockPeriod <= HEIGHT
  sigmaProp(Pk && isMature)
}