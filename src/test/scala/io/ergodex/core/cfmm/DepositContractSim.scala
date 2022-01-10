package io.ergodex.core.cfmm

class DepositContractSim(pool: CfmmPool) {

  def eval(inputX: Long, inputY: Long)(outputLP: Long, change: Long): Boolean = {
    val minByX = BigInt(inputX) * pool.supplyLP / pool.x
    println(s"minByX: $minByX")
    val minByY = BigInt(inputY) * pool.supplyLP / pool.y
    println(s"minByY: $minByY")
    val minimalReward = minByX min minByY
    println(s"minimalReward: $minimalReward")

    val validTokenChange =
      if (minByX < minByY) {
        println("[minByX < minByY]")
        val diff = minByY - minByX
        println(s"diff: $diff")
        val excessY = diff * pool.y / pool.supplyLP
        println(s"excessY: $excessY")
        change >= excessY
      } else if (minByX > minByY) {
        println("[minByX > minByY]")
        val diff = minByX - minByY
        println(s"diff: $diff")
        val excessX = diff * pool.x / pool.supplyLP
        println(s"excessX: $excessX")
        change >= excessX
      } else if (minByX == minByY) {
        println("[minByX == minByY]")
        true
      } else {
        false
      }

    validTokenChange && outputLP >= minimalReward
  }
}
