package io.ergodex.core

import cats.Eval
import cats.data.State
import io.ergodex.core.DebugContract.Ledger
import io.ergodex.core.syntax.{CollOpaque, SigmaProp}
import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction, JsonCodecs}
import scorex.util.encode.Base16
import sigmastate.Values.ConstantNode
import sigmastate.interpreter.ContextExtension
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

  def pullIOs(tx: ErgoLikeTransaction): (List[(ErgoBox, ContextExtension)], List[ErgoBox]) = {
    val inputs = tx.inputs.flatMap(i => pullBox(i.boxId).map(_ -> i.spendingProof.extension))
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

final case class RuntimeSetup[B[_[_]]](box: B[Ledger], ctx: RuntimeCtx) {
  def run(implicit ev: B[Ledger] <:< BoxSim[Ledger]): Eval[Boolean] = ev(box).validator.run(ctx).map(_._2)
}

object RuntimeSetup extends JsonCodecs {
  def fromIOs[Box[_[_]]](
    inputs: List[(ErgoBox, ContextExtension)],
    outputs: List[ErgoBox],
    selfInputIx: Int,
    height: Int
  )(implicit fromBox: TryFromBox[Box, Ledger]): Option[RuntimeSetup[Box]] = {
    val (selfIn, ext) = inputs(selfInputIx)
    for {
      selfBox <- fromBox.tryFromBox(selfIn)
      ctx = RuntimeCtx(
        height,
        vars = ext.values.toVector.map { case (ix, c) =>
          ix.toInt -> (c match {
            case ConstantNode(array: special.collection.CollOverArray[Any @unchecked], _) =>
              CollOpaque(array.toArray.toVector)
            case ConstantNode(p @ sigmastate.eval.CSigmaProp(_), _) => SigmaProp(p.propBytes.toArray.toVector)
            case ConstantNode(v, _)                                 => v
            case v                                                  => v
          })
        }.toMap,
        inputs  = inputs.map(_._1).map(AnyBox.tryFromBox.tryFromBox).collect { case Some(x) => x },
        outputs = outputs.map(AnyBox.tryFromBox.tryFromBox).collect { case Some(x) => x }
      )
    } yield RuntimeSetup(selfBox, ctx)
  }
}