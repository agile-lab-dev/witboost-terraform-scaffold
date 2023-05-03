package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.config.AsynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, DescriptorValidator, Validation }
import it.agilelab.spinframework.app.features.provision.{
  CloudProvider,
  ComponentToken,
  ProvisionResult,
  ProvisioningStatus
}
import it.agilelab.spinframework.app.features.status.GetStatus

class AsynchronousMockDependencies extends AsynchronousSpecificProvisionerDependencies {
  override def descriptorValidator: DescriptorValidator = _ => Validation.start

  override def cloudProvider: CloudProvider = new CloudProvider {
    override def provision(descriptor: ComponentDescriptor): ProvisionResult   =
      ProvisionResult.running(ComponentToken("component-1234"))
    override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = ProvisionResult.completed()
  }

  override def getStatus: GetStatus = _ => ProvisioningStatus.Completed
}

object AsynchronousSpMock extends SpecificProvisioner {
  override val specificProvisionerDependencies: AsynchronousSpecificProvisionerDependencies =
    new AsynchronousMockDependencies
}
