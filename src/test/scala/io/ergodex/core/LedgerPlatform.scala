package io.ergodex.core

import cats.Eval
import cats.data.State
import io.ergodex.core.DebugContract.Ledger
import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction, JsonCodecs}
import scorex.util.encode.Base16
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.okhttp.OkHttpSyncBackend
import io.circe.{Decoder, Json, Printer}
import io.circe.derivation.deriveDecoder
import sttp.model.MediaType

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

  private def pullBox(id: BoxId): Option[ErgoBox] = basicRequest
    .post(uri"https://gql.ergoplatform.com/")
    .body(bodyJson(id))
    .response(asJson[Data])
    .send(backend)
    .body
    .right
    .toOption
    .flatMap(_.data.boxes.headOption)

  def body(id: BoxId): String =
    s"""{"query":"{boxes(boxId:\\"${Base16.encode(
      id
    )}\\") {boxId,value,creationHeight,transactionId,index,ergoTree,additionalRegisters,assets{tokenId,amount,}}}"}"""

  def bodyJson(id: BoxId): Json = io.circe.parser.parse(body(id)).toOption.get

  // curl 'https://gql.ergoplatform.com/' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'DNT: 1' -H 'Origin: https://gql.ergoplatform.com' --data-binary '{"query":"{\n  boxes(boxId: \"e8cb8e8acbebab6b9ba3706904facd70393f5731c7e36a55caa660fde5419b60\") {\n    boxId,\n    value,\n    creationHeight,\n    index,\n    ergoTree,\n    additionalRegisters,\n    assets {\n      tokenId,\n      amount,\n    }\n  }\n}"}' --compressed

  implicit val jsonSerializer: BodySerializer[Json] = json =>
    StringBody(json.printWith(Printer.noSpaces), "utf-8", MediaType.ApplicationJson)

  case class Boxes(boxes: List[ErgoBox])

  object Boxes {
    implicit val decoder: Decoder[Boxes] = deriveDecoder
  }

  case class Data(data: Boxes)

  object Data {
    implicit val decoder: Decoder[Data] = deriveDecoder
  }
}

final case class RuntimeSetup[B[_[_]]](box: B[Ledger], ctx: RuntimeCtx) {
  def run(implicit ev: B[Ledger] <:< BoxSim[Ledger]): Eval[Boolean] = ev(box).validator.run(ctx).map(_._2)
}

object RuntimeSetup extends JsonCodecs {
  def fromIOs[Box[_[_]]](
    inputs: List[ErgoBox],
    outputs: List[ErgoBox],
    selfInputIx: Int,
    height: Int
  )(implicit fromBox: TryFromBox[Box, Ledger]): Option[RuntimeSetup[Box]] = {
    val selfIn = inputs(selfInputIx)
    for {
      selfBox <- fromBox.tryFromBox(selfIn)
      ctx = RuntimeCtx(
        height,
        inputs  = inputs.map(AnyBox.tryFromBox.tryFromBox).collect { case Some(x) => x },
        outputs = outputs.map(AnyBox.tryFromBox.tryFromBox).collect { case Some(x) => x }
      )
    } yield RuntimeSetup(selfBox, ctx)
  }
}
