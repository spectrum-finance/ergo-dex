package io.ergodex.core.sim

import io.ergodex.core.sim.lqmining.simple.DepositBox
import org.ergoplatform.ErgoLikeTransaction

object DebugContract extends App with LedgerPlatform {
  val json =
    """
      |{
      |    "id": "730d9ce6bbf31403d2225c666979110ae0d7aaf47492e634932047c8b09a654d",
      |    "inputs": [
      |        {
      |            "boxId": "3c323cc2d400cba7959e1c35aca5c2ada1b1be1eb4a12918318946765e35e803",
      |            "spendingProof": {
      |                "proofBytes": "",
      |                "extension": {}
      |            }
      |        },
      |        {
      |            "boxId": "f6e8e393238df45040c88d9dc8372638c1dc18745f98dc9efbc585d524a5a605",
      |            "spendingProof": {
      |                "proofBytes": "",
      |                "extension": {}
      |            }
      |        }
      |    ],
      |    "dataInputs": [],
      |    "outputs": [
      |        {
      |            "boxId": "0324a97696e05aca2418ffc105f7dc1f5a50581ef2f1bbab4c5953479e62e3e5",
      |            "value": 1250000,
      |            "ergoTree": "19e9041f04000402040204040404040604060408040804040402040004000402040204000400040a0500040204020500050004020402040605000500040205000500d81bd601b2a5730000d602db63087201d603db6308a7d604e4c6a70410d605e4c6a70505d606e4c6a70605d607b27202730100d608b27203730200d609b27202730300d60ab27203730400d60bb27202730500d60cb27203730600d60db27202730700d60eb27203730800d60f8c720a02d610998c720902720fd6118c720802d612b27204730900d6139a99a37212730ad614b27204730b00d6159d72137214d61695919e72137214730c9a7215730d7215d617b27204730e00d6187e721705d6199d72057218d61a998c720b028c720c02d61b998c720d028c720e02d1ededededed93b27202730f00b27203731000ededed93e4c672010410720493e4c672010505720593e4c6720106057206928cc77201018cc7a70193c27201c2a7ededed938c7207018c720801938c7209018c720a01938c720b018c720c01938c720d018c720e0193b172027311959172107312eded929a997205721172069c7e9995907216721772169a721773137314057219937210f0721a939c7210997218a273157e721605f0721b958f72107316ededec929a997205721172069c7e9995907216721772169a72177317731805721992a39a9a72129c72177214b2720473190093721af0721092721b959172167217731a9c721a997218a2731b7e721605d801d61ce4c672010704edededed90721c997216731c909972119c7e997217721c0572199a7219720693f0998c72070272117d9d9c7e7219067e721b067e720f0605937210731d93721a731e",
      |            "assets": [
      |                {
      |                    "tokenId": "fbaafdf0842aaf8678143c6cc42b70ee4bcbd339f5b8fb69ea70b1995bfd1aed",
      |                    "amount": 1
      |                },
      |                {
      |                    "tokenId": "0779ec04f2fae64e87418a1ad917639d4668f78484f45df962b0dec14a2591d2",
      |                    "amount": 10000
      |                },
      |                {
      |                    "tokenId": "98da76cecb772029cfec3d53727d5ff37d5875691825fbba743464af0c89ce45",
      |                    "amount": 1591
      |                },
      |                {
      |                    "tokenId": "3fdce3da8d364f13bca60998c20660c79c19923f44e141df01349d2e63651e86",
      |                    "amount": 9998509
      |                },
      |                {
      |                    "tokenId": "c256908dd9fd477bde350be6a41c0884713a1b1d589357ae731763455ef28c10",
      |                    "amount": 99985889
      |                }
      |            ],
      |            "additionalRegisters": {
      |                "R4": "1004d00f1486b26fd00f",
      |                "R6": "05d00f",
      |                "R5": "05a09c01"
      |            },
      |            "creationHeight": 913749,
      |            "transactionId": "730d9ce6bbf31403d2225c666979110ae0d7aaf47492e634932047c8b09a654d",
      |            "index": 0
      |        },
      |        {
      |            "boxId": "22e81b62a95b5df72c5989b8f68a0630d92a055e4ff4013cb761201f2c74047d",
      |            "value": 250000,
      |            "ergoTree": "0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec",
      |            "assets": [
      |                {
      |                    "tokenId": "3c323cc2d400cba7959e1c35aca5c2ada1b1be1eb4a12918318946765e35e803",
      |                    "amount": 9223372036854775806
      |                }
      |            ],
      |            "additionalRegisters": {},
      |            "creationHeight": 913749,
      |            "transactionId": "730d9ce6bbf31403d2225c666979110ae0d7aaf47492e634932047c8b09a654d",
      |            "index": 1
      |        },
      |        {
      |            "boxId": "89b4a1f3f6ff487e3943468872d35496f498ff353e4203cf7efa9f4a27a3a66d",
      |            "value": 250000,
      |            "ergoTree": "19b20315040004000404040404040402040004000500040204020400050204040400040205000404040005fcffffffffffffffff010100d80ed601b2a5730000d602db63087201d603e4c6a7050ed604b2a4730100d605db63087204d6068cb2720573020002d607998cb27202730300027206d608e4c6a7040ed609db6308a7d60a8cb2720973040001d60bb27209730500d60cb27209730600d60d8c720c02d60e8c720b02d1ed938cb27202730700017203959372077308d808d60fb2a5e4e3000400d610b2a5e4e3010400d611db63087210d612b27211730900d613b2e4c672040410730a00d614c672010804d6157e99721395e67214e47214e4c67201070405d616b2db6308720f730b00eded93c2720f7208edededededed93e4c67210040e720893e4c67210050e720393c27210c2a7938602720a730cb27211730d00938c7212018c720b019399720e8c72120299720e9c7215720d93b27211730e00720ced938c7216018cb27205730f0001927e8c721602069d9c9c7e9de4c6720405057e721305067e720d067e999d720e720d7215067e720606958f7207731093b2db6308b2a47311007312008602720a73137314",
      |            "assets": [
      |                {
      |                    "tokenId": "3fdce3da8d364f13bca60998c20660c79c19923f44e141df01349d2e63651e86",
      |                    "amount": 308
      |                },
      |                {
      |                    "tokenId": "c256908dd9fd477bde350be6a41c0884713a1b1d589357ae731763455ef28c10",
      |                    "amount": 2464
      |                },
      |                {
      |                    "tokenId": "3c323cc2d400cba7959e1c35aca5c2ada1b1be1eb4a12918318946765e35e803",
      |                    "amount": 1
      |                }
      |            ],
      |            "additionalRegisters": {
      |                "R4": "0e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec",
      |                "R5": "0e20fbaafdf0842aaf8678143c6cc42b70ee4bcbd339f5b8fb69ea70b1995bfd1aed"
      |            },
      |            "creationHeight": 913749,
      |            "transactionId": "730d9ce6bbf31403d2225c666979110ae0d7aaf47492e634932047c8b09a654d",
      |            "index": 2
      |        },
      |        {
      |            "boxId": "9679303cfca7f1009f54ca990570702d6faf701e4fb850d580d92b9aa66eff37",
      |            "value": 1250000,
      |            "ergoTree": "0008cd02d6b2141c21e4f337e9b065a031a6269fb5a49253094fc6243d38662eb765db00",
      |            "assets": [],
      |            "additionalRegisters": {},
      |            "creationHeight": 913749,
      |            "transactionId": "730d9ce6bbf31403d2225c666979110ae0d7aaf47492e634932047c8b09a654d",
      |            "index": 3
      |        },
      |        {
      |            "boxId": "fd71eca991a5fc869fc3e086562ddaa110761822db9fa0868151473850347c1f",
      |            "value": 1000000,
      |            "ergoTree": "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304",
      |            "assets": [],
      |            "additionalRegisters": {},
      |            "creationHeight": 913749,
      |            "transactionId": "730d9ce6bbf31403d2225c666979110ae0d7aaf47492e634932047c8b09a654d",
      |            "index": 4
      |        }
      |    ]
      |}
      |""".stripMargin

  val Right(tx) = io.circe.parser.decode[ErgoLikeTransaction](json)

  val (inputs, outputs) = pullIOs(tx)

  val Some(setup) = RuntimeSetup.fromIOs[DepositBox, Ledger](inputs, outputs, 1, 913700)(DepositBox.tryFromBox)

  val res = setup.box.validator.run(setup.ctx)

  println(res.value)
}
