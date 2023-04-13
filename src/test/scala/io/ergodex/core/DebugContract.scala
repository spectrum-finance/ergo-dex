package io.ergodex.core

import io.ergodex.core.cfmm3.n2t.SwapBuyBox
import io.ergodex.core.lqmining.simple.LqMiningPoolBoxSelfHosted
import org.ergoplatform.ErgoLikeTransaction

object DebugContract extends App with LedgerPlatform {
  val json =
    """
      |{
      |  "id": "1fc6dee76b900842c71e75201a75a26e7bc156ad83b988fd380b4af21cddf10e",
      |  "inputs": [
      |    {
      |      "boxId": "8c45c202e7d663fa4eb06aac6c2bd9ce13bd38b06a4fdc2406bc12e97fabfd5c",
      |      "spendingProof": {
      |        "proofBytes": "",
      |        "extension": {}
      |      }
      |    },
      |    {
      |      "boxId": "c9c1456f8c2972762e004b9ddb19d88c81b263532110d8c3976428aed6f0ed50",
      |      "spendingProof": {
      |        "proofBytes": "",
      |        "extension": {}
      |      }
      |    },
      |    {
      |      "boxId": "ab1fe501ee88eb03bb65332cd0da759bc59c0267b72568f45b5778b4163bf1ee",
      |      "spendingProof": {
      |        "proofBytes": "8f868c5e0745af192e8f195cf687aef2e4ece9a8b659e6db388119547c9e457e877342e44e573a687dea2f9726471367cbe7d2931bea1fdf",
      |        "extension": {}
      |      }
      |    }
      |  ],
      |  "dataInputs": [],
      |  "outputs": [
      |    {
      |      "boxId": "e5db50e6a25c1a16079e70a45e92ef6019f9b1ce28de2e11062e38d7e414c01e",
      |      "value": 37253101914612,
      |      "ergoTree": "1999030f0400040204020404040405feffffffffffffffff0105feffffffffffffffff01050004d00f040004000406050005000580dac409d819d601b2a5730000d602e4c6a70404d603db63087201d604db6308a7d605b27203730100d606b27204730200d607b27203730300d608b27204730400d6099973058c720602d60a999973068c7205027209d60bc17201d60cc1a7d60d99720b720cd60e91720d7307d60f8c720802d6107e720f06d6117e720d06d612998c720702720fd6137e720c06d6147308d6157e721206d6167e720a06d6177e720906d6189c72117217d6199c72157217d1ededededededed93c27201c2a793e4c672010404720293b27203730900b27204730a00938c7205018c720601938c7207018c72080193b17203730b9593720a730c95720e929c9c721072117e7202069c7ef07212069a9c72137e7214067e9c720d7e72020506929c9c721372157e7202069c7ef0720d069a9c72107e7214067e9c72127e7202050695ed720e917212730d907216a19d721872139d72197210ed9272189c721672139272199c7216721091720b730e",
      |      "assets": [
      |        {
      |          "tokenId": "1d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f",
      |          "amount": 1
      |        },
      |        {
      |          "tokenId": "fa6326a26334f5e933b96470b53b45083374f71912b0d7597f00c2c7ebeb5da6",
      |          "amount": 9223372012237844599
      |        },
      |        {
      |          "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
      |          "amount": 78381654
      |        }
      |      ],
      |      "creationHeight": 939705,
      |      "additionalRegisters": {
      |        "R4": "04c60f"
      |      },
      |      "transactionId": "1fc6dee76b900842c71e75201a75a26e7bc156ad83b988fd380b4af21cddf10e",
      |      "index": 0
      |    },
      |    {
      |      "boxId": "669fbcf0b4480b1603f1960e02019cf09dc375f49aee1e77eb48528749de5c9b",
      |      "value": 8713845,
      |      "ergoTree": "0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec",
      |      "assets": [
      |        {
      |          "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
      |          "amount": 2
      |        }
      |      ],
      |      "creationHeight": 939705,
      |      "additionalRegisters": {},
      |      "transactionId": "1fc6dee76b900842c71e75201a75a26e7bc156ad83b988fd380b4af21cddf10e",
      |      "index": 1
      |    },
      |    {
      |      "boxId": "b36a87fb9824d43d1958908f3f00fa745bee711a84394a7c5f1843fc52770222",
      |      "value": 1642650905,
      |      "ergoTree": "0008cd02ddbe95b7f88d47bd8c2db823cc5dd1be69a650556a44d4c15ac65e1d3e34324c",
      |      "assets": [
      |        {
      |          "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
      |          "amount": 10
      |        }
      |      ],
      |      "creationHeight": 939705,
      |      "additionalRegisters": {},
      |      "transactionId": "1fc6dee76b900842c71e75201a75a26e7bc156ad83b988fd380b4af21cddf10e",
      |      "index": 2
      |    },
      |    {
      |      "boxId": "ecb15f4f25efd4d894267ef46e6e2805c49f9c7d46e63dc06d09a8813621192f",
      |      "value": 2000000,
      |      "ergoTree": "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304",
      |      "assets": [],
      |      "creationHeight": 939705,
      |      "additionalRegisters": {},
      |      "transactionId": "1fc6dee76b900842c71e75201a75a26e7bc156ad83b988fd380b4af21cddf10e",
      |      "index": 3
      |    }
      |  ]
      |}
      |""".stripMargin

  val Right(tx) = io.circe.parser.decode[ErgoLikeTransaction](json)

  val (inputs, outputs) = pullIOs(tx)

  val Some(setup) = RuntimeSetup.fromIOs[SwapBuyBox](inputs, outputs, 1, 939705)

  println(setup.run.value)
}
