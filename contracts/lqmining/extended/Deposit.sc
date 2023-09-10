{ // ===== Contract Information ===== //
  // Name: Deposit (extended LM)
  // Description: Contract that validates user's deposit into the extended LM Pool.
  //
  // ===== Deposit Box ===== //
  // Tokens:
  //   0:
  //     _1: LQ Token ID  // identifier for the stake pool box.
  //     _2: Amount of LQ Tokens to deposit
  //
  // Constants:
  // {1}  -> PoolId[Coll[Byte]]
  // {2}  -> RedeemerProp[Coll[Byte]]
  // {3}  -> RefundPk[ProveDlog]
  // {12} -> BundlePropHash[Coll[Byte]]
  // {16} -> ExpectedNumEpochs[Int]
  // {20} -> MinerPropBytes[Coll[Byte]]
  // {23} -> MaxMinerFee[Long]
  //
  // ErgoTree: 19a2041904000e2002020202020202020202020202020202020202020202020202020202020202020e20000000000000000000000000000000000000000000000000000000000000000008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040c040204040400040005fcffffffffffffffff0104000e200508f3623d4b2be3bdb9737b3e65644f011167eefb830d9965205f022ceda40d04060400040804140402050204040e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d803d601b2a4730000d6027301d6037302eb027303d195ed92b1a4730490b1db630872017305d805d604db63087201d605b2a5730600d606c57201d607b2a5730700d6088cb2db6308a773080002ededed938cb27204730900017202ed93c2720572039386027206730ab2db63087205730b00ededededed93cbc27207730c93d0e4c672070608720393e4c67207070e72029386028cb27204730d00017208b2db63087207730e009386028cb27204730f00019c72087e731005b2db6308720773110093860272067312b2db6308720773130090b0ada5d90109639593c272097314c1720973157316d90109599a8c7209018c72090273177318
  //
  // ErgoTreeTemplate: d803d601b2a4730000d6027301d6037302eb027303d195ed92b1a4730490b1db630872017305d805d604db63087201d605b2a5730600d606c57201d607b2a5730700d6088cb2db6308a773080002ededed938cb27204730900017202ed93c2720572039386027206730ab2db63087205730b00ededededed93cbc27207730c93d0e4c672070608720393e4c67207070e72029386028cb27204730d00017208b2db63087207730e009386028cb27204730f00019c72087e731005b2db6308720773110093860272067312b2db6308720773130090b0ada5d90109639593c272097314c1720973157316d90109599a8c7209018c72090273177318
  //
  // ErgoTreeTemplateHash: 9125d9488d38c942ab3a6a212c05f9c0d8d7fe6c3cb85c3638459e432a99cfbb
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
  // ===== Getting INPUTS data ===== //
  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size >= 2 && poolIn.tokens.size <= 6) {
      // ===== Getting SELF data ===== //
      val deposit = SELF.tokens(0)

      // ===== Getting INPUTS data ===== //
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
          bundleOut.R6[SigmaProp].get.propBytes == RedeemerProp &&
          bundleOut.R7[Coll[Byte]].get == PoolId &&
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
