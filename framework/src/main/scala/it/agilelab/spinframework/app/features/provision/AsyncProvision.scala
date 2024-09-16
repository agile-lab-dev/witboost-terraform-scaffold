package it.agilelab.spinframework.app.features.provision

import cats.effect.IO
import com.typesafe.config.Config
import io.circe.Json
import it.agilelab.spinframework.app.api.generated.definitions.ProvisionInfo
import it.agilelab.spinframework.app.config.Configuration.provisionerConfig
import it.agilelab.spinframework.app.features.compiler.YamlDescriptor

trait AsyncProvision {
  def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config = provisionerConfig): IO[ProvisionResult]
  def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config = provisionerConfig): IO[ProvisionResult]
  def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config = provisionerConfig): IO[ProvisionResult]
  def doValidate(yamlDescriptor: YamlDescriptor): IO[ProvisionResult]
  def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): IO[ProvisionResult]
}

object AsyncProvision {
  def fromSyncProvision(provision: Provision): AsyncProvision = new AsyncProvision {
    override def doProvisioning(yamlDescriptor: YamlDescriptor, cfg: Config): IO[ProvisionResult] =
      IO.blocking(provision.doProvisioning(yamlDescriptor, cfg))

    override def doUnprovisioning(yaml: YamlDescriptor, removeData: Boolean, cfg: Config): IO[ProvisionResult] =
      IO.blocking(provision.doUnprovisioning(yaml, removeData, cfg))

    override def doUpdateAcl(provisionInfo: ProvisionInfo, refs: Set[String], cfg: Config): IO[ProvisionResult] =
      IO.blocking(provision.doUpdateAcl(provisionInfo, refs))

    override def doValidate(yamlDescriptor: YamlDescriptor): IO[ProvisionResult] =
      IO.blocking(provision.doValidate(yamlDescriptor))

    override def doReverse(useCaseTemplateId: String, catalogInfo: Json, inputParams: Json): IO[ProvisionResult] =
      IO.blocking(provision.doReverse(useCaseTemplateId, catalogInfo, inputParams))
  }
}
