package io.ergodex.core.sim

import cats.Functor
import cats.mtl.Ask
import cats.mtl.syntax.ask._
import tofu.WithContext
import tofu.syntax.monadic._

final case class SigmaProp(value: String)

final case class RuntimeCtx(
  height: Int,
  vars: Map[Int, Any]                        = Map.empty,
  inputs: List[Box[BoxRuntime.NonRunnable]]  = List.empty,
  outputs: List[Box[BoxRuntime.NonRunnable]] = List.empty,
  signatories: List[SigmaProp]               = List.empty
)

object RuntimeCtx extends WithContext.Companion[RuntimeCtx] {
  def init: RuntimeCtx            = at(1)
  def at(height: Int): RuntimeCtx = RuntimeCtx(height = height)
}

trait RuntimeState[F[_]] {
  def withRuntimeState[R](fn: RuntimeCtx => R): F[R]
}

object RuntimeState {
  def withRuntimeState[F[_], R](fn: RuntimeCtx => R)(implicit ev: RuntimeState[F]): F[R] =
    ev.withRuntimeState(fn)

  implicit def ledgerStateFromContext[F[_]: Functor: RuntimeCtx.Has]: RuntimeState[F] =
    new RuntimeState[F] {
      def withRuntimeState[R](fn: RuntimeCtx => R): F[R] =
        RuntimeCtx.access.map(fn)
    }

  implicit def ledgerStateFromAsk[F[_]: Functor: Ask[*[_], RuntimeCtx]]: RuntimeState[F] =
    new RuntimeState[F] {
      def withRuntimeState[R](fn: RuntimeCtx => R): F[R] =
        fn.reader
    }
}
