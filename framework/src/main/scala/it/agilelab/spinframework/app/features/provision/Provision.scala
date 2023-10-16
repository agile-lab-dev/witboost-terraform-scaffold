package it.agilelab.spinframework.app.features.provision

import com.typesafe.config.Config
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.features.compiler.{ JsonDescriptor, YamlDescriptor }

trait Provision {
  def doProvisioning(yamlDescriptor: YamlDescriptor): ProvisionResult
  def doUnprovisioning(yaml: YamlDescriptor): ProvisionResult
  def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config = provisionerConfig): ProvisionResult
}
