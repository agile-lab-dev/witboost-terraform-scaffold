package it.agilelab.spinframework.app.cloudprovider

import io.circe.Json
import it.agilelab.spinframework.app.cloudprovider.CloudProviderStub.reverse
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, InputParams }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

object CloudProviderStub {
  type ProvisionFunction   = (ComponentDescriptor, Set[String]) => ProvisionResult
  type UnprovisionFunction = (ComponentDescriptor, Set[String], Boolean) => ProvisionResult
  type UpdateAclFcuntion   = (ComponentDescriptor, ComponentDescriptor, Set[String]) => ProvisionResult
  type ValidateFunction    = ComponentDescriptor => ProvisionResult
  type ReverseFunction     = (String, ComponentDescriptor, InputParams) => ProvisionResult

  def provision(function: ProvisionFunction): CloudProviderStub     = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult =
      function.apply(descriptor, mappedOwners)
  }
  def unprovision(function: UnprovisionFunction): CloudProviderStub = new CloudProviderStub {
    override def unprovision(
      descriptor: ComponentDescriptor,
      mappedOwners: Set[String],
      removeData: Boolean
    ): ProvisionResult =
      function.apply(descriptor, mappedOwners, removeData)
  }
  def validate(function: ValidateFunction): CloudProviderStub       = new CloudProviderStub {
    override def validate(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }

  def reverse(function: ReverseFunction): CloudProviderStub = new CloudProviderStub {
    override def reverse(
      useCaseTemplateId: String,
      catalogInfo: ComponentDescriptor,
      inputParams: InputParams
    ): ProvisionResult = function.apply(useCaseTemplateId, catalogInfo, inputParams)
  }

  def updateAcl(function: UpdateAclFcuntion): CloudProviderStub = new CloudProviderStub {
    override def updateAcl(
      resultDescriptor: ComponentDescriptor,
      requestDescriptor: ComponentDescriptor,
      refs: Set[String]
    ): ProvisionResult =
      function.apply(resultDescriptor, requestDescriptor, refs)
  }

  def alwaysReturnCompleted: CloudProviderStub = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult =
      ProvisionResult.completed()
    override def unprovision(
      descriptor: ComponentDescriptor,
      mappedOwners: Set[String],
      removeData: Boolean
    ): ProvisionResult                                                                                  =
      ProvisionResult.completed()
  }
}

class CloudProviderStub extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult =
    throw new UnsupportedOperationException
  override def unprovision(
    descriptor: ComponentDescriptor,
    mappedOwners: Set[String],
    removeData: Boolean
  ): ProvisionResult                                                                                  =
    throw new UnsupportedOperationException
  override def updateAcl(
    resultDescriptor: ComponentDescriptor,
    requestDescriptor: ComponentDescriptor,
    refs: Set[String]
  ): ProvisionResult                                                                                  =
    throw new UnsupportedOperationException

  override def validate(descriptor: ComponentDescriptor): ProvisionResult = throw new UnsupportedOperationException

  override def reverse(
    useCaseTemplateId: String,
    catalogInfo: ComponentDescriptor,
    inputParams: InputParams
  ): ProvisionResult = throw new UnsupportedOperationException
}
