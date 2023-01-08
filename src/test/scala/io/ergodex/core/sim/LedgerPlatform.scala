package io.ergodex.core.sim

import cats.data.State
import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction, JsonCodecs}
import scorex.util.encode.Base16
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.okhttp.OkHttpSyncBackend

trait LedgerPlatform extends JsonCodecs {

  private lazy val backend = OkHttpSyncBackend()

  type Ledger[A] = State[RuntimeCtx, A]

  implicit val ledgerStateFromLedger: RuntimeState[Ledger] =
    new RuntimeState[Ledger] {
      def withRuntimeState[R](fn: RuntimeCtx => R): Ledger[R] =
        State.get.map(fn)
    }

  object Ledger {
    def extendBy(n: Int): Ledger[Unit] = State.modify(s0 => s0.copy(height = s0.height + n))
    def extend: Ledger[Unit]           = extendBy(1)
    def ctx: Ledger[RuntimeCtx]        = State.get
  }

  def pullIOs(tx: ErgoLikeTransaction): (List[ErgoBox], List[ErgoBox]) = {
    val inputs = tx.inputs.flatMap(i => pullBox(i.boxId))
    inputs.toList -> tx.outputs.toList
  }

  def pullBox(id: BoxId): Option[ErgoBox] = basicRequest
    .get(uri"http://213.239.193.208:9053/utxo/byId/${Base16.encode(id)}")
    .response(asJson[ErgoBox])
    .send(backend)
    .body
    .right
    .toOption
}
