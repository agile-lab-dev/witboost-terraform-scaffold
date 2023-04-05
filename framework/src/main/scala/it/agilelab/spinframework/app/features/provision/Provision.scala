package it.agilelab.spinframework.app.features.provision

import it.agilelab.spinframework.app.features.compiler.YamlDescriptor

trait Provision {
  def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult
  def doUnprovisioning(yaml: YamlDescriptor): ProvisionResult
}
