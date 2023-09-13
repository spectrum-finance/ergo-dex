{ // Name: DepositBudget.
  // Description: Contract that validates budget deposit into the extended LM Pool.
  //
  // ===== Deposit Box ===== //
  // Tokens:
  //   0:
  //     _1: Budget(Reward) Token ID
  //     _2: Amount of Reward Tokens to deposit
  //
  // Constants:
  //   {1}  -> RefundPk[ProveDlog]
  //   {5}  -> PoolId[Coll[Byte]]
  //   {6}  -> BudgetTokenInd[Int]
  //   {8}  -> RedeemerPropBytes[Coll[Byte]]
  //   {12} -> MinerPropBytes[Coll[Byte]]
  //   {15} -> MaxMinerFee[Long]
  //
  // Validations:
  // 1. Rewards are deposited into the correct LM Pool;
  // 2. Correct amount of rewards are deposited and redeemerPropBytes matches with one of LM the Pool redeemers;
  // 3. Miner Fee is valid.
  //
  // Limitations:
  // 1. Rewards can only be deposited by specific budgetRedeemers who are stored in the LM Pool;
  // 2. Deposits with main/optional rewards must be separate.
  //
  // Deposit budget Tx:
  //    INPUTS:  (0 -> pool_in,
  //              1 -> deposit_budget_in).
  //    OUTPUTS: (0 -> pool_out).
  //
  // ErgoTree: 19a60311040008cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d0404040c04000e20020202020202020202020202020202020202020202020202020202020202020204000402040a04000e20000000000000000000000000000000000000000000000000000000000000000004000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005a09c010100d801d601b2a4730000eb027301d195ed92b1a4730290b1db630872017303d806d602db63087201d603938cb27202730400017305d604e4e30004d60595937204730673077308d606b27202720500d607b2db6308a7730900eded7203ededed720393730ad0b2e4c672010614720400938c7206018c72070193998cb2db6308b2a5730b00720500028c7206028c72070290b0ada5d90108639593c27208730cc17208730d730ed90108599a8c7208018c720802730f7310
  //
  // ErgoTreeTemplate: d801d601b2a4730000eb027301d195ed92b1a4730290b1db630872017303d806d602db63087201d603938cb27202730400017305d604e4e30004d60595937204730673077308d606b27202720500d607b2db6308a7730900eded7203ededed720393730ad0b2e4c672010614720400938c7206018c72070193998cb2db6308b2a5730b00720500028c7206028c72070290b0ada5d90108639593c27208730cc17208730d730ed90108599a8c7208018c720802730f7310
  //
  // ErgoTreeTemplateHash: 3c9b030883cb4fdf9a03dbe8e63d33cbfb26e73dccec33a9c99ff7e67f73b1be
  //
  // ===== Getting INPUTS data ===== //
  val poolIn = INPUTS(0)

  val validDeposit =
    if (INPUTS.size == 2 && poolIn.tokens.size == 6) {
      // ===== Getting SELF data ===== //
      val deposit = SELF.tokens(0)

      // ===== Getting OUTPUTS data ===== //
      val poolOut = OUTPUTS(0)

      // ===== Validating conditions ===== //
      // 1.
      val validPoolIn = poolIn.tokens(0)._1 == PoolId
      // 2.
      val validPoolOut = {
        val budgetRedeemers   = poolIn.R6[Coll[SigmaProp]].get
        val budgetDelta       = poolOut.tokens(BudgetTokenInd)._2 - poolIn.tokens(BudgetTokenInd)._2
        val budgetRedeemerInd = if (BudgetTokenInd == 1) 0 else 1

        (poolIn.tokens(0)._1 == PoolId) &&
        (RedeemerPropBytes == budgetRedeemers(budgetRedeemerInd).propBytes) &&
        (poolIn.tokens(BudgetTokenInd)._1 == deposit._1) &&
        (budgetDelta == deposit._2)
      }
      // 3.
      val validMinerFee = OUTPUTS
        .map { (o: Box) =>
          if (o.propositionBytes == MinerPropBytes) o.value else 0L
        }
        .fold(0L, { (a: Long, b: Long) => a + b }) <= MaxMinerFee

      validPoolIn && validPoolOut && validMinerFee

    } else false

  sigmaProp(RefundPk || validDeposit)
}
