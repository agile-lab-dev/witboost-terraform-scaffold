package it.agilelab.spinframework.app.features.provision

import it.agilelab.spinframework.app.features.compiler._
import it.agilelab.spinframework.app.features.compiler.{ Compile, CompileResult, YamlDescriptor }

class ProvisionService(compile: Compile, cloudProvider: CloudProvider) extends Provision {

  override def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = {
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)
    cloudProvider.provision(result.descriptor)
  }

  override def doUnprovisioning(yamlDescriptor: YamlDescriptor): ProvisionResult = {
    val result: CompileResult = compile.doCompile(yamlDescriptor)
    if (!result.isSuccess) return ProvisionResult.failure(result.errors)
    cloudProvider.unprovision(result.descriptor)
  }
}
