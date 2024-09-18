package it.agilelab.spinframework.app.features.provision

import io.circe.Json
import it.agilelab.spinframework.app.features.compiler.{ ComponentDescriptor, ImportBlock, InputParams }

/** This trait represents the operations performed on cloud provider to
  * actually create all resources required by the provision request.
  *
  * As a client of the framework you must extend this trait and provide
  * your own implementation, by accessing the fields within the input descriptor
  * and using their values to forward a proper request to your cloud provider.
  */
trait CloudProvider {

  /** Interprets the descriptor and translates it to a provision request for the cloud provider.
    *
    * @param descriptor contains the details of the resources to allocate on the cloud
    * @param mappedOwners the mapped principals that are owners of the DP
    * @return the state of the operation
    */
  def provision(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult

  /** Receives the component descriptor and forwards an unprovision request to the cloud provider.
    *
    * @param descriptor contains the details of the resources to unprovision on the cloud
    * @param mappedOwners the mapped principals that are owners of the DP
    * @param removeData wethere to remove the underlying data or not
    * @return the state of the operation
    */
  def unprovision(descriptor: ComponentDescriptor, mappedOwners: Set[String], removeData: Boolean): ProvisionResult

  /** Receives the component descriptor and forwards a validate request to the cloud provider.
    *
    * @param descriptor contains the details of the resources to validate on the cloud
    * @param mappedOwners the mapped principals that are owners of the DP
    * @return the state of the operation
    */
  def validate(descriptor: ComponentDescriptor, mappedOwners: Set[String]): ProvisionResult

  /** Receives the descriptor and the subjects and forwards an updateAcl request to the cloud provider.
    *
    * @param descriptor contains the details of the resources to allocate on the cloud
    * @param refs is the list of subjects received by the provisioning coordinator
    * @return the state of the operation
    */
  def updateAcl(
    resultDescriptor: ComponentDescriptor,
    requestDescriptor: ComponentDescriptor,
    refs: Set[String]
  ): ProvisionResult

  /** Receives the catalogInfo and Input Params and forwards a reverse request to the cloud provider.
    *
    * @param useCaseTemplateId is the id of the use useCaseTemplate mapped with the reverse provisioning template
    * @param catalogInfo contains the catalog info of the component to reverse provision
    * @param inputParams contains the list of input parameters
    * @return the state of the operation
    */
  def reverse(
    useCaseTemplateId: String,
    catalogInfo: ComponentDescriptor,
    inputParams: InputParams
  ): ProvisionResult
}
