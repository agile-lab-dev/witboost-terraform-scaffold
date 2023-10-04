package it.agilelab.spinframework.app.cloudprovider

import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

object CloudProviderStub {
  type ProvisionFunction = ComponentDescriptor => ProvisionResult
  type UpdateAclFcuntion = (ComponentDescriptor, Set[String]) => ProvisionResult

  def provision(function: ProvisionFunction): CloudProviderStub   = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }
  def unprovision(function: ProvisionFunction): CloudProviderStub = new CloudProviderStub {
    override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }
  def updateAcl(function: UpdateAclFcuntion): CloudProviderStub   = new CloudProviderStub {
    override def updateAcl(descriptor: ComponentDescriptor, refs: Set[String]): ProvisionResult =
      function.apply(descriptor, refs)
  }

  def alwaysReturnCompleted: CloudProviderStub = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult   = ProvisionResult.completed()
    override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = ProvisionResult.completed()
  }
}

class CloudProviderStub extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor): ProvisionResult                    = throw new UnsupportedOperationException
  override def unprovision(descriptor: ComponentDescriptor): ProvisionResult                  = throw new UnsupportedOperationException
  override def updateAcl(descriptor: ComponentDescriptor, refs: Set[String]): ProvisionResult =
    throw new UnsupportedOperationException
}
