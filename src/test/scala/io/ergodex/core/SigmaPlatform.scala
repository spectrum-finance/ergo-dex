package io.ergodex.core

import org.ergoplatform.ErgoAddressEncoder
import scorex.crypto.hash.{Blake2b256, Sha256}
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.Terms.ValueOps
import sigmastate.lang.{CompilerSettings, SigmaCompiler, TransformingSigmaBuilder}
import sigmastate.serialization.ErgoTreeSerializer

trait SigmaPlatform {

  implicit private val IR: IRContext = new CompiletimeIRContext()

  val sigma: SigmaCompiler =
    SigmaCompiler(
      CompilerSettings(
        networkPrefix    = ErgoAddressEncoder.MainnetNetworkPrefix,
        builder          = TransformingSigmaBuilder,
        lowerMethodCalls = true
      )
    )

  def readSource(path: String): String = {
    val source = scala.io.Source.fromFile(path)
    try source.mkString
    finally source.close()
  }

  def updateVersionHeader(tree: ErgoTree): ErgoTree = {
    val versionHeader = ErgoTree.headerWithVersion(version = 1)
    val header =
      if (ErgoTree.isConstantSegregation(tree.header)) (versionHeader | ErgoTree.ConstantSegregationFlag).toByte
      else versionHeader
    tree.copy(header = header)
  }

  def compile(source: String, env: Map[String, Any]): ErgoTree =
    updateVersionHeader(
      ErgoTree.fromProposition(sigma.compile(env, source).asSigmaProp)
    )

  def printTree(signature: String, source: String, env: Map[String, Any]): Unit = {
    val tree = compile(source, env)

    println(s"[$signature] Constants:")
    tree.constants.zipWithIndex.foreach { case (c, i) => println(s"{$i} -> $c") }
    println("* " * 40)
    println(
      s"[$signature] ErgoTree:         " + Base16.encode(ErgoTreeSerializer.DefaultSerializer.serializeErgoTree(tree))
    )
    println(
      s"[$signature] ErgoTreeTemplateHash:         " + Base16.encode(
        Blake2b256.hash(Sha256.hash(tree.template))
      )
    )
    println(s"[$signature] ErgoTreeTemplate: " + Base16.encode(tree.template))
    println("-" * 80)
    println()
  }
}
