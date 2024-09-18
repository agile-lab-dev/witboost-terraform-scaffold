package it.agilelab.provisionermock.config

import io.circe.Json
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, InputParams }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult }

class MockCloudProvider extends CloudProvider {
  override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult = {
    println("######### /provision #########")
    println(descriptor)
    ProvisionResult.completed()
  }

  override def unprovision(
    descriptor: ComponentDescriptor,
    mappedOwners: Set[String],
    removeData: Boolean
  ): ProvisionResult = {
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

  override def validate(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult = {
    println("######### /validate #########")
    println(descriptor)
    ProvisionResult.completed()
  }

  override def reverse(
    useCaseTemplateId: String,
    catalogInfo: ComponentDescriptor,
    inputParams: InputParams
  ): ProvisionResult = {
    println("######### /reverse-provisioning #########")
    println(catalogInfo)
    ProvisionResult.completed()
  }
}
