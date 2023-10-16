package it.agilelab.provisionermock

import it.agilelab.provisionermock.config.{ MockCloudProvider, MockDescriptorValidator }
import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.config.SynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.DescriptorValidator
import it.agilelab.spinframework.app.features.provision.CloudProvider

class SynchronousMockDependencies extends SynchronousSpecificProvisionerDependencies {
  override def descriptorValidator: DescriptorValidator                       = new MockDescriptorValidator
  override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(new MockCloudProvider)
}

object SynchronousSpMock extends SpecificProvisioner {
  override val specificProvisionerDependencies: SynchronousSpecificProvisionerDependencies =
    new SynchronousMockDependencies
}
