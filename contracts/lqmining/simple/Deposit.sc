{ // ===== Contract Information ===== //
  // Name: Deposit
  // Description: Contract that validates user's deposit into the LM Pool.
  //
  // ===== Deposit Box ===== //
  // Tokens:
  //   0:
  //     _1: LQ Token ID  // identifier for the stake pool box.
  //     _2: Amount of LQ Tokens to deposit
  //
  // Constants:
  // {1}  -> PoolId[Coll[Byte]]
  // {3}  -> RedeemerProp[Coll[Byte]]
  // {6}  -> RefundPk[ProveDlog]
  // {10} -> BundlePropHash[Coll[Byte]]
  // {14} -> ExpectedNumEpochs[Int]
  // {18} -> MinerPropBytes[Coll[Byte]]
  // {21} -> MaxMinerFee[Long]
  //
  // ErgoTree: 1988041604000e20020202020202020202020202020202020202020202020202020202020202020204020e2000000000000000000000000000000000000000000000000000000000000000000404040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d040005fcffffffffffffffff0104000e20eadb8f9e5f452ea6b252292f4b97b172f35cd52af8f75d3acc7c2087a5b2fab504060400040804140402050204040e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c01d808d601b2a4730000d602db63087201d6037301d604b2a5730200d6057303d606c57201d607b2a5730400d6088cb2db6308a773050002eb027306d1ededed938cb27202730700017203ed93c27204720593860272067308b2db63087204730900ededededed93cbc27207730a93d0e4c672070408720593e4c67207050e72039386028cb27202730b00017208b2db63087207730c009386028cb27202730d00019c72087e730e05b2db63087207730f0093860272067310b2db6308720773110090b0ada5d90109639593c272097312c1720973137314d90109599a8c7209018c7209027315
  //
  // ErgoTreeTemplate: d808d601b2a4730000d602db63087201d6037301d604b2a5730200d6057303d606c57201d607b2a5730400d6088cb2db6308a773050002eb027306d1ededed938cb27202730700017203ed93c27204720593860272067308b2db63087204730900ededededed93cbc27207730a93d0e4c672070408720593e4c67207050e72039386028cb27202730b00017208b2db63087207730c009386028cb27202730d00019c72087e730e05b2db63087207730f0093860272067310b2db6308720773110090b0ada5d90109639593c272097312c1720973137314d90109599a8c7209018c7209027315
  //
  // Validations:
  // 1. Assets are deposited into the correct LM Pool;
  // 2. Redeemer PubKey matches and correct Bundle Key token amount token is received;
  // 3. Bundle stores correct: Script; RedeemerProp; PoolId; vLQ token amount; TMP token amount; Bundle Key token amount.
  // 4. Miner Fee is valid.
  //
  // Limitations:
  // 1. Assets can only be locked in the Bundle with a specific BundlePropHash;
  // 2. User receives 0x7fffffffffffffffL - 1L bundleKeyId tokens to identify his own unique Bundle;
  // 3. RedeemerProp stored in Bundle is SigmaProp type to ensure the distribution of rewards;
  // 4. If corresponding to RedeemerProp address exists, but was specified incorrectly, then the rewards will go to this address.
  //
  // ===== Getting SELF data ===== //
  val deposit = SELF.tokens(0)

  // ===== Getting INPUTS data ===== //
  val poolIn      = INPUTS(0)
  val bundleKeyId = poolIn.id

  // ===== Getting OUTPUTS data ===== //
  val redeemerOut = OUTPUTS(1) // 0 -> pool_out, 1 -> redeemer_out, 2 -> bundle_out
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
    bundleOut.R4[SigmaProp].get.propBytes == RedeemerProp &&
    bundleOut.R5[Coll[Byte]].get == PoolId &&
    (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(0) &&
    (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(1) &&
    (bundleKeyId, 1L) == bundleOut.tokens(2)
  }
  // 4.
  val validMinerFee = OUTPUTS
    .map { (o: Box) =>
      if (o.propositionBytes == MinerPropBytes) o.value else 0L
    }
    .fold(0L, {(a: Long, b: Long) => a + b}) <= MaxMinerFee

  sigmaProp(RefundPk || (validPoolIn && validRedeemerOut && validBundle && validMinerFee))
}
