package io.ergodex.core

import cats.data.State

trait LedgerPlatform {

  type Ledger[A] = State[RuntimeCtx, A]

  implicit val ledgerStateFromLedger: RuntimeState[Ledger] =
    new RuntimeState[Ledger] {
      def withRuntimeState[R](fn: RuntimeCtx => R): Ledger[R] =
        State.get.map(fn)
    }

  object Ledger {
    def extendBy(n: Int): Ledger[Unit] = State.modify(s0 => s0.copy(height = s0.height + n))
    def extend: Ledger[Unit]           = extendBy(1)
    def ctx: Ledger[RuntimeCtx]         = State.get
  }
}
