package io.ergodex.core.locking

import io.ergodex.core.SigmaPlatform
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.serialization.ErgoTreeSerializer

class TokenLockSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with SigmaPlatform {

  property("Contract compiles") {
    val source   = readSource("contracts/locking/TokenLock.sc")
    val env      = Map(
      "Pk"         -> DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage,
      "LockPeriod" -> 10
    )
    printTree("TokenLock", source, env)
  }
}
