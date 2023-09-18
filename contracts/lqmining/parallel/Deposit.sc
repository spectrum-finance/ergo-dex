{ // ===== Contract Information ===== //
  // Name: Deposit (parallel LM).
  // Description: Contract that validates user's deposit into the parallel LM Pool.
  //
  // ===== Deposit Box ===== //
  // Tokens:
  //   0:
  //     _1: LQ Token ID  // identifier for the stake pool box.
  //     _2: Amount of LQ Tokens to deposit
  //
  // Constants:
  //    {1}  -> PoolId[Coll[Byte]]
  //    {2}  -> RedeemerProp[Coll[Byte]]
  //    {3}  -> RefundPk[ProveDlog]
  //    {12} -> BundlePropHash[Coll[Byte]]
  //    {16} -> ExpectedNumEpochs[Int]
  //    {20} -> MinerPropBytes[Coll[Byte]]
  //    {23} -> MaxMinerFee[Long]
  //
  // Validations:
  // 1. Assets are deposited into the correct LM Pool;
  // 2. Redeemer PubKey matches and correct Bundle Key token amount token is received;
  // 3. Bundle stores correct: Script; PoolId; vLQ token amount;
  //                           TMP token amount; Bundle Key token amount.
  // 4. Miner Fee is valid.
  //
  // Limitations:
  // 1. Assets can only be locked in the Bundle with a specific BundlePropHash.
  // 2. User receives 0x7fffffffffffffffL - 1L bundleKeyId tokens to identify his own unique Bundle.
  //
  // Deposit Tx:
  //    INPUTS:  (0 -> pool_in,
  //              1 -> deposit_in).
  //    OUTPUTS: (0 -> pool_out,
  //              1 -> redeemer_out,
  //              2 -> bundle_out).
  //
  // ErgoTree: 1993041904000e20020202020202020202020202020202020202020202020202020202020202020208cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040c04020404040004000e20000000000000000000000000000000000000000000000000000000000000000005fcffffffffffffffff0104000e2003189434843364096423232bca66502cc63a362d5fdc90c4aee5312b3dc865b304060400040804140402050204040e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d802d601b2a4730000d6027301eb027302d195ed93b1a4730393b1db630872017304d805d603db63087201d604b2a5730500d605c57201d606b2a5730600d6078cb2db6308a773070002ededed938cb27203730800017202ed93c2720473099386027205730ab2db63087204730b00edededed93cbc27206730c93e4c67206060e72029386028cb27203730d00017207b2db63087206730e009386028cb27203730f00019c72077e731005b2db6308720673110093860272057312b2db6308720673130090b0ada5d90108639593c272087314c1720873157316d90108599a8c7208018c72080273177318
  //
  // ErgoTreeTemplate: d802d601b2a4730000d6027301eb027302d195ed93b1a4730393b1db630872017304d805d603db63087201d604b2a5730500d605c57201d606b2a5730600d6078cb2db6308a773070002ededed938cb27203730800017202ed93c2720473099386027205730ab2db63087204730b00edededed93cbc27206730c93e4c67206060e72029386028cb27203730d00017207b2db63087206730e009386028cb27203730f00019c72077e731005b2db6308720673110093860272057312b2db6308720673130090b0ada5d90108639593c272087314c1720873157316d90108599a8c7208018c72080273177318
  //
  // ErgoTreeTemplateHash: 135d7c5a61c0203773dae467597d4d5f5de71eb7ccc1e5c3ec78a80e0983d50c
  //
  // ===== Getting INPUTS data ===== //
  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size == 2 && poolIn.tokens.size == 6) {
      // ===== Getting SELF data ===== //
      val deposit = SELF.tokens(0)

      // ===== Getting INPUTS data ===== //
      val bundleKeyId = poolIn.id

      // ===== Getting OUTPUTS data ===== //
      val redeemerOut = OUTPUTS(1)
      val bundleOut   = OUTPUTS(2)

      // ===== Calculations ===== //
      val expectedVLQ       = deposit._2
      val expectedNumEpochs = ExpectedNumEpochs
      val expectedTMP       = expectedVLQ * expectedNumEpochs

      // ===== Validating conditions ===== //
      // 1.
      val validPoolIn = poolIn.tokens(0)._1 == PoolId
      // 2.
      val validRedeemerOut =
        redeemerOut.propositionBytes == RedeemerProp &&
        (bundleKeyId, 0x7fffffffffffffffL - 1L) == redeemerOut.tokens(0)
      // 3.
      val validBundle = {
        blake2b256(bundleOut.propositionBytes) == BundlePropHash &&
        bundleOut.R6[Coll[Byte]].get == PoolId &&
        (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(0) &&
        (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(1) &&
        (bundleKeyId, 1L) == bundleOut.tokens(2)
      }
      // 4.
      val validMinerFee = OUTPUTS
        .map { (o: Box) =>
          if (o.propositionBytes == MinerPropBytes) o.value else 0L
        }
        .fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn && validRedeemerOut && validBundle && validMinerFee

    } else false

  sigmaProp(RefundPk || validDeposit)
}
