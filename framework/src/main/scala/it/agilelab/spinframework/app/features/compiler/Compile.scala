package it.agilelab.spinframework.app.features.compiler

trait Compile {
  def doCompile(yamlDescriptor: YamlDescriptor): CompileResult
}
