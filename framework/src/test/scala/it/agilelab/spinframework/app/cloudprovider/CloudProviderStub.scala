package it.agilelab.spinframework.app.cloudprovider

import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

object CloudProviderStub {
  type ProvisionFunction   = ComponentDescriptor => ProvisionResult
  type UnprovisionFunction = (ComponentDescriptor, Boolean) => ProvisionResult
  type UpdateAclFcuntion   = (ComponentDescriptor, ComponentDescriptor, Set[String]) => ProvisionResult
  type ValidateFunction    = ComponentDescriptor => ProvisionResult

  def provision(function: ProvisionFunction): CloudProviderStub     = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }
  def unprovision(function: UnprovisionFunction): CloudProviderStub = new CloudProviderStub {
    override def unprovision(descriptor: ComponentDescriptor, removeData: Boolean): ProvisionResult =
      function.apply(descriptor, removeData)
  }
  def validate(function: ValidateFunction): CloudProviderStub       = new CloudProviderStub {
    override def validate(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }
  def updateAcl(function: UpdateAclFcuntion): CloudProviderStub     = new CloudProviderStub {
    override def updateAcl(
      resultDescriptor: ComponentDescriptor,
      requestDescriptor: ComponentDescriptor,
      refs: Set[String]
    ): ProvisionResult =
      function.apply(resultDescriptor, requestDescriptor, refs)
  }

  def alwaysReturnCompleted: CloudProviderStub = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult                        = ProvisionResult.completed()
    override def unprovision(descriptor: ComponentDescriptor, removeData: Boolean): ProvisionResult =
      ProvisionResult.completed()
  }
}

class CloudProviderStub extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor): ProvisionResult                        = throw new UnsupportedOperationException
  override def unprovision(descriptor: ComponentDescriptor, removeData: Boolean): ProvisionResult =
    throw new UnsupportedOperationException
  override def updateAcl(
    resultDescriptor: ComponentDescriptor,
    requestDescriptor: ComponentDescriptor,
    refs: Set[String]
  ): ProvisionResult                                                                              =
    throw new UnsupportedOperationException

  override def validate(descriptor: ComponentDescriptor): ProvisionResult = throw new UnsupportedOperationException
}
