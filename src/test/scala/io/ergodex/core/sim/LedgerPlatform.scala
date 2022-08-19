package io.ergodex.core.sim

import cats.data.State

trait LedgerPlatform {

  type Ledger[A] = State[LedgerCtx, A]

  implicit val ledgerStateFromLedger: LedgerState[Ledger] =
    new LedgerState[Ledger] {
      def withLedgerState[R](fn: LedgerCtx => R): Ledger[R] =
        State.get.map(fn)
    }

  object Ledger {
    def extendBy(n: Int): Ledger[Unit] = State.modify(s0 => s0.copy(height = s0.height + n))
    def extend: Ledger[Unit]           = extendBy(1)
    def ctx: Ledger[LedgerCtx]         = State.get
  }
}
