{ // ===== Contract Information ===== //
  // Name: RedeemBudget.
  // Description: Contract that validates program budget redeem from the parallel LM Pool.
  //
  // Constants:
  //  {0}  -> RefundPk[ProveDlog]
  //  {5}  -> RedeemerPropBytes[Coll[Byte]]
  //  {6}  -> ExpectedBudget[Coll[Byte]]
  //  {7}  -> ExpectedBudgetAmount[Long]
  //  {9}  -> MinerPropBytes[Coll[Byte]]
  //  {12} -> MaxMinerFee[Long]
  //
  // Validations:
  // 1. Redeemer out is valid: redeemer PubKey matches PubKey stored in Bundle Box;
  //                           correct vLQ token amount is received.
  // 2. Miner Fee is valid.
  //
  // Redeem Budget Tx:
  //    INPUTS:  (0 -> pool_in,
  //              1 -> redeem_budget_in).
  //    OUTPUTS: (0 -> pool_out,
  //              1 -> redeemer_out).
  //
  // ErgoTree: 19db021008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04040400040c0400040c04020e2001010101010101010101010101010101010101010101010101010101010101010e20000000000000000000000000000000000000000000000000000000000000000005d00f04000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100eb027300d195eded93b1a4730193b1db6308b2a4730200730393b1db6308b2a57304007305d801d601b2a5730600eded93c27201730793860273087309b2db63087201730a0090b0ada5d90102639593c27202730bc17202730c730dd90102599a8c7202018c720202730e730f
  //
  // ErgoTreeTemplate: eb027300d195eded93b1a4730193b1db6308b2a4730200730393b1db6308b2a57304007305d801d601b2a5730600eded93c27201730793860273087309b2db63087201730a0090b0ada5d90102639593c27202730bc17202730c730dd90102599a8c7202018c720202730e730f
  //
  // ErgoTreeTemplateHash: dca3ba3e0128d7eca86f20e4f80d961cdac7b294831c59bcd248a9adce5a799c
  //
  // ===== Getting INPUTS data ===== //
  val poolIn  = INPUTS(0)
  val poolOut = OUTPUTS(0)

  val validRedeem =
    if (INPUTS.size == 2 && poolIn.tokens.size == 6 && poolOut.tokens.size == 6) {
      // ===== Getting OUTPUTS data ===== //
      val redeemerOut = OUTPUTS(1)

      // ===== Validating conditions ===== //
      // 1.
      val validRedeemerOut =
        (redeemerOut.propositionBytes == RedeemerPropBytes) &&
        ((ExpectedBudget, ExpectedBudgetAmount) == redeemerOut.tokens(0))
      // 2.
      val validMinerFee = OUTPUTS
        .map { (o: Box) =>
          if (o.propositionBytes == MinerPropBytes) o.value else 0L
        }
        .fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validRedeemerOut && validMinerFee

    } else false

  sigmaProp(RefundPk || validRedeem)
}
