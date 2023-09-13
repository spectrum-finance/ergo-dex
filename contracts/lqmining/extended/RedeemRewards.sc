{ // ===== Contract Information ===== //
  // Name: RedeemRewards.
  // Description: Contract that validates user's redeem from the Bundle.
  //
  // Tokens:
  //   0:
  //     _1: BundleKeyId
  //     _2: 0x7fffffffffffffffL - 1L
  //
  // Constants:
  //  {0}  -> RefundPk[ProveDlog]
  //  {3}  -> RedeemerPropBytes[Coll[Byte]]
  //  {6}  -> ExpectedReward[Coll[Byte]]
  //  {7}  -> ExpectedRewardAmount[Long]
  //  {9}  -> MinerPropBytes[Coll[Byte]]
  //  {12} -> MaxMinerFee[Long]
  //
  // Validations:
  // 1. Redeemer out is valid: RedeemerPropBytes matches redeemer_out.propositionBytes;
  //                           bundleKeyId tokens are preserved;
  //                           expected Reward tokens and their amount is received.
  // 2. Miner Fee is valid.
  //
  // Redeem Rewards Tx:
  //    INPUTS:  (0 -> bundle_in,
  //              1 -> redeem_rewards_in).
  //    OUTPUTS: (0 -> bundle_out,
  //              1 -> redeemer_out).
  //
  // ErgoTree: 19a2041904000e2002020202020202020202020202020202020202020202020202020202020202020e20000000000000000000000000000000000000000000000000000000000000000008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040c040204040400040005fcffffffffffffffff0104000e200508f3623d4b2be3bdb9737b3e65644f011167eefb830d9965205f022ceda40d04060400040804140402050204040e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d803d601b2a4730000d6027301d6037302eb027303d195ed92b1a4730490b1db630872017305d805d604db63087201d605b2a5730600d606c57201d607b2a5730700d6088cb2db6308a773080002ededed938cb27204730900017202ed93c2720572039386027206730ab2db63087205730b00ededededed93cbc27207730c93d0e4c672070608720393e4c67207070e72029386028cb27204730d00017208b2db63087207730e009386028cb27204730f00019c72087e731005b2db6308720773110093860272067312b2db6308720773130090b0ada5d90109639593c272097314c1720973157316d90109599a8c7209018c72090273177318
  //
  // ErgoTreeTemplate: d803d601b2a4730000d6027301d6037302eb027303d195ed92b1a4730490b1db630872017305d805d604db63087201d605b2a5730600d606c57201d607b2a5730700d6088cb2db6308a773080002ededed938cb27204730900017202ed93c2720572039386027206730ab2db63087205730b00ededededed93cbc27207730c93d0e4c672070608720393e4c67207070e72029386028cb27204730d00017208b2db63087207730e009386028cb27204730f00019c72087e731005b2db6308720773110093860272067312b2db6308720773130090b0ada5d90109639593c272097314c1720973157316d90109599a8c7209018c72090273177318
  //
  // ErgoTreeTemplateHash: 9125d9488d38c942ab3a6a212c05f9c0d8d7fe6c3cb85c3638459e432a99cfbb
  //
  // ===== Getting INPUTS data ===== //
  val validRedeem =
    if (INPUTS.size == 2) {
      // ===== Getting OUTPUTS data ===== //
      val redeemerOut = OUTPUTS(1)

      // ===== Validating conditions ===== //
      // 1.
      val validRedeemerOut = {
        (redeemerOut.propositionBytes == RedeemerPropBytes) &&
        (SELF.tokens(0) == redeemerOut.tokens(0)) &&
        ((ExpectedReward, ExpectedRewardAmount) == redeemerOut.tokens(1))
      }
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
