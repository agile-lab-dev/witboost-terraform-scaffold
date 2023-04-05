package it.agilelab.provisionermock

import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.DescriptorValidator
import it.agilelab.spinframework.app.features.provision.CloudProvider
import it.agilelab.provisionermock.config.{ MockCloudProvider, MockDescriptorValidator }

class SynchronousMockDependencies extends SynchronousSpecificProvisionerDependencies {
  override def descriptorValidator: DescriptorValidator = new MockDescriptorValidator
  override def cloudProvider: CloudProvider             = new MockCloudProvider
}

object SynchronousSpMock extends SpecificProvisioner {
  override val specificProvisionerDependencies: SynchronousSpecificProvisionerDependencies =
    new SynchronousMockDependencies
}
