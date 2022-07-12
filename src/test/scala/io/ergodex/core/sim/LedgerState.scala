package io.ergodex.core.sim

import cats.Functor

import tofu.WithContext
import tofu.syntax.monadic._

final case class LedgerCtx(height: Int)

object LedgerCtx extends WithContext.Companion[LedgerCtx]

trait LedgerState[F[_]] {
  def withLedgerState[R](fn: LedgerCtx => R): F[R]
}

object LedgerState {
  def withLedgerState[F[_], R](fn: LedgerCtx => R)(implicit ev: LedgerState[F]): F[R] =
    ev.withLedgerState(fn)

  implicit def ledgerStateFromContext[F[_]: Functor: LedgerCtx.Has]: LedgerState[F] =
    new LedgerState[F] {
      def withLedgerState[R](fn: LedgerCtx => R): F[R] =
        LedgerCtx.access.map(fn)
    }
}
