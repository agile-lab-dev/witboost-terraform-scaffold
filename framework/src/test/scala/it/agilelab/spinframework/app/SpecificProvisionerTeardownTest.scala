package it.agilelab.spinframework.app

import it.agilelab.spinframework.app.config.{
  SpecificProvisionerDependencies,
  SynchronousSpecificProvisionerDependencies
}
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, DescriptorValidator, Validation }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class SpecificProvisionerTeardownTest
    extends AnyFlatSpec
    with should.Matchers
    with BeforeAndAfterAll
    with SpecificProvisioner {

  override def specificProvisionerDependencies: SpecificProvisionerDependencies =
    new SynchronousSpecificProvisionerDependencies {
      override def descriptorValidator: DescriptorValidator = _ => Validation.start

      override def cloudProvider: CloudProvider = new CloudProvider {
        override def provision(descriptor: ComponentDescriptor): ProvisionResult = ProvisionResult.completed()

        override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = ProvisionResult.completed()
      }
    }

  "The TestSpecificProvisioner" should "stop correctly" in {
    this.main(Array.empty)
    Thread.sleep(1000)

    noException should be thrownBy this.teardown()
  }

}
