package it.agilelab.spinframework.app.features.compiler

case class InputParams(importBlocks: Seq[ImportBlock], skipSafetyChecks: Boolean)
case class ImportBlock(to: String, id: String)
