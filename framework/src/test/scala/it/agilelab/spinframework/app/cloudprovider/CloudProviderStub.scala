package it.agilelab.spinframework.app.cloudprovider

import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

object CloudProviderStub {
  type ProvisionFunction = ComponentDescriptor => ProvisionResult

  def provision(function: ProvisionFunction): CloudProviderStub   = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }
  def unprovision(function: ProvisionFunction): CloudProviderStub = new CloudProviderStub {
    override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = function.apply(descriptor)
  }

  def alwaysReturnCompleted: CloudProviderStub = new CloudProviderStub {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult   = ProvisionResult.completed()
    override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = ProvisionResult.completed()
  }
}

class CloudProviderStub extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor): ProvisionResult   = throw new UnsupportedOperationException
  override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = throw new UnsupportedOperationException
}
