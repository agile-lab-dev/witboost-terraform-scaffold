package it.agilelab.spinframework.app.features.compiler

trait Compile {
  def doCompile(descriptor: Descriptor): CompileResult
}
