package it.agilelab.provisionermock

import cats.effect.IO
import it.agilelab.spinframework.app.SpecificProvisioner
import it.agilelab.spinframework.app.config.AsynchronousSpecificProvisionerDependencies
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, DescriptorValidator, Validation }
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ComponentToken, ProvisionResult }
import it.agilelab.spinframework.app.features.status.{ CacheRepository, GetStatus, TaskRepository }

import scala.concurrent.ExecutionContext

class AsynchronousMockDependencies extends AsynchronousSpecificProvisionerDependencies {
  override def descriptorValidator: DescriptorValidator = _ => Validation.start

  override def cloudProvider(moduleId: String): Either[String, CloudProvider] = Right(new CloudProvider {
    override def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult =
      ProvisionResult.running(ComponentToken("component-1234"))
    override def unprovision(
      descriptor: ComponentDescriptor,
      mappedOwners: Set[String],
      removeData: Boolean
    ): ProvisionResult                                                                                  =
      ProvisionResult.completed()
    override def updateAcl(
      descriptorResult: ComponentDescriptor,
      descriptorRequest: ComponentDescriptor,
      refs: Set[String]
    ): ProvisionResult                                                                                  =
      ProvisionResult.completed()
    override def validate(descriptor: ComponentDescriptor): ProvisionResult                             = ProvisionResult.completed()
  })

  override def getStatus: GetStatus = _ => IO.pure(Some(ProvisionResult.completed()))

  override def taskRepository: TaskRepository = new CacheRepository

  override def executionContext: ExecutionContext = ExecutionContext.global
}

object AsynchronousSpMock extends SpecificProvisioner {
  override val specificProvisionerDependencies: AsynchronousSpecificProvisionerDependencies =
    new AsynchronousMockDependencies
}
