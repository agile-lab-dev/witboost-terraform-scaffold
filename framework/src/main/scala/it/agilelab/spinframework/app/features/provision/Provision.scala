package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.Config
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.features.compiler.YamlDescriptor

trait Provision {
  def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config = provisionerConfig): ProvisionResult
  def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config = provisionerConfig): ProvisionResult
  def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config = provisionerConfig): ProvisionResult
  def doValidate(yamlDescriptor: YamlDescriptor): ProvisionResult
}
