package it.agilelab.provisionermock.config

import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

class MockCloudProvider extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor): ProvisionResult = {
    println("######### /provision #########")
    println(descriptor)
    ProvisionResult.completed()
  }

  override def unprovision(descriptor: ComponentDescriptor): ProvisionResult = {
    println("######### /unprovision #########")
    println(descriptor)
    ProvisionResult.completed()
  }
}
