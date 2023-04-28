package io.ergodex.core

import io.ergodex.core.lqmining.simple.{StakingBundleBox}
import org.ergoplatform.ErgoLikeTransaction

object DebugContract extends App with LedgerPlatform {
  val json =
    """|Place tx here|""".stripMargin

  val Right(tx) = io.circe.parser.decode[ErgoLikeTransaction](json)

  val (inputs, outputs) = pullIOs(tx)

  val Some(setupPool) = RuntimeSetup.fromIOs[StakingBundleBox](inputs, outputs, 3, 935823)

  println(setupPool.run.value)
}