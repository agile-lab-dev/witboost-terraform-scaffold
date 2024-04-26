package it.agilelab.provisionermock.config

import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

class MockCloudProvider extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult = {
    println("######### /provision #########")
    println(descriptor)
    ProvisionResult.completed()
  }

  override def unprovision(descriptor: ComponentDescriptor, removeData: Boolean): ProvisionResult = {
    println("######### /unprovision #########")
    println(descriptor)
    ProvisionResult.completed()
  }

  override def updateAcl(
    resultDescriptor: ComponentDescriptor,
    requestDescriptor: ComponentDescriptor,
    refs: Set[String]
  ): ProvisionResult = {
    println("######### /updateacl #########")
    println(resultDescriptor)
    println(requestDescriptor)
    ProvisionResult.completed()
  }

  override def validate(descriptor: ComponentDescriptor): ProvisionResult = {
    println("######### /validate #########")
    println(descriptor)
    ProvisionResult.completed()
  }
}
