package it.agilelab.spinframework.app.config

import cats.effect.IO
import it.agilelab.spinframework.app.features.compiler.DescriptorValidator
import it.agilelab.spinframework.app.features.provision.{ CloudProvider, ProvisionResult, ProvisioningStatus }
import it.agilelab.spinframework.app.features.status.{ GetStatus, TaskRepository }

import scala.concurrent.ExecutionContext

/** Abstract factory that allows to specify the implementations of the framework extension points.
  */
sealed trait SpecificProvisionerDependencies {

  /** Specifies the descriptor's validation logic.
    */
  def descriptorValidator: DescriptorValidator

  /** Interacts with a cloud provider to provision/unprovision a component.
    */
  def cloudProvider(moduleId: String): Either[String, CloudProvider]

  /** Returns the provisioning status of a component.
    */
  def getStatus: GetStatus

}

/** Extend this trait to specify a synchronous model for a specific provisioner.
  *
  * The method getStatus returns [[ProvisioningStatus.Completed]] as a default implementation:
  * a synchronous specific provisioner does not need to retrieve the provisioning status information
  * since it already waits for its completion as default semantic.
  */
trait SynchronousSpecificProvisionerDependencies extends SpecificProvisionerDependencies {
  override def getStatus: GetStatus = _ => IO.pure(Some(ProvisionResult.completed()))
}

/** Extend this trait to specify an asynchronous model for a specific provisioner.
  *
  * The method getStatus needs to be implemented in order to return the [[ProvisioningStatus]]
  * of a provisioning request, given the identifier of a component.
  */
trait AsynchronousSpecificProvisionerDependencies extends SpecificProvisionerDependencies {
  def taskRepository: TaskRepository
  def executionContext: ExecutionContext
}
