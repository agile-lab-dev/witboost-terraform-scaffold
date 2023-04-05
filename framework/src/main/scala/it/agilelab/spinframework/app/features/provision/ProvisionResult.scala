package it.agilelab.spinframework.app.features.provision

import it.agilelab.spinframework.app.features.compiler.ErrorMessage

object ProvisionResult {

  def completed(): ProvisionResult = completed(ComponentToken(""))

  def completed(componentToken: ComponentToken): ProvisionResult =
    ProvisionResult(ProvisioningStatus.Completed, componentToken, Seq.empty)

  def failure(errors: Seq[ErrorMessage]): ProvisionResult =
    ProvisionResult(ProvisioningStatus.Failed, ComponentToken(""), errors)

  def running(token: ComponentToken): ProvisionResult =
    ProvisionResult(ProvisioningStatus.Running, token, Seq.empty)
}

/** This class is the result of a provisioning operation.
  *
  * It can represent any of the following three cases:
  *  - completed: the provision has been performed synchronously and successfully
  *  - failed: the provision has been performed synchronously but unsuccessfully
  *  - running: the provision will be performed asynchronously and a token is returned
  */
case class ProvisionResult(
  provisioningStatus: ProvisioningStatus,
  componentToken: ComponentToken,
  errors: Seq[ErrorMessage]
) {

  /** Returns true if the provisioning of the component is successful.
    */
  def isSuccessful: Boolean = provisioningStatus != ProvisioningStatus.Failed
}
