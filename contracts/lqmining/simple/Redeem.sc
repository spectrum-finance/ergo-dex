{ // ===== Contract Information ===== //
  // Name: Redeem
  // Description: Contract that validates user's redeem from the LM Pool.
  //
  // Tokens:
  //   0:
  //     _1: BundleKeyId
  //     _2: 0x7fffffffffffffffL - 1L
  //
  // Constants:
  //  {0} -> RefundPk[ProveDlog]
  //  {5} -> MinerPropBytes[Coll[Byte]]
  //  {8} -> MaxMinerFee[Long]
  //  {9} -> RedeemerProp[Coll[Byte]]
  //  {10} -> ExpectedLQ[Coll[Byte]]
  //  {11} -> ExpectedLQAmount[Long]
  //
  // ErgoTree: 19d1020e08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040400040a04020e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010e2001010101010101010101010101010101010101010101010101010101010101010e20000000000000000000000000000000000000000000000000000000000000000005d00f04000100eb027300d195ed92b1a4730193b1db6308b2a47302007303d802d601b2a5730400d60290b0ada5d90102639593c272027305c1720273067307d90102599a8c7202018c7202027308ededed93c272017309938602730a730bb2db63087201730c0072027202730d
  //
  // ErgoTreeTemplate: eb027300d195ed92b1a4730193b1db6308b2a47302007303d802d601b2a5730400d60290b0ada5d90102639593c272027305c1720273067307d90102599a8c7202018c7202027308ededed93c272017309938602730a730bb2db63087201730c0072027202730d
  //
  // ErgoTreeTemplateHash: f624989acb298aeb9094312d89e9902061df72598639d55ae320db6a99b26c5a
  //
  // Validations:
  // 1. Redeemer out is valid: Redeemer PubKey matches PubKey stored in Bundle Box; correct vLQ token amount received;
  // 2. Miner Fee is valid.
  //
  // ===== Getting INPUTS data ===== //
  val poolIn = INPUTS(0)

  val validRedeem =
    if (INPUTS.size >= 2 && poolIn.tokens.size == 5) {
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
        .fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validRedeemerOut && validMinerFee && validMinerFee

    } else false

  sigmaProp(RefundPk || validRedeem)
}
