package it.agilelab.spinframework.app.features.provision

import it.agilelab.spinframework.app.features.compiler.ComponentDescriptor

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
    *  @param removeData wethere to remove the underlying data or not
    * @return the state of the operation
    */
  def unprovision(descriptor: ComponentDescriptor, removeData: Boolean): ProvisionResult

  /** Receives the component descriptor and forwards a validate request to the cloud provider.
    *
    * @param descriptor contains the details of the resources to validate on the cloud
    * @return the state of the operation
    */
  def validate(descriptor: ComponentDescriptor): ProvisionResult

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
}
