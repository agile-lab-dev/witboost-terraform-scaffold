package it.agilelab.spinframework.app.features.provision

import io.circe.Json
import it.agilelab.spinframework.app.api.generated.definitions.Log
import it.agilelab.spinframework.app.features.compiler.{ ErrorMessage, TerraformOutput }

object ProvisionResult {

  def completed(): ProvisionResult = completed(ComponentToken(""), outputs = Seq.empty)

  def completed(outputs: Seq[TerraformOutput]): ProvisionResult = completed(ComponentToken(""), outputs = outputs)

  def completed(componentToken: ComponentToken, outputs: Seq[TerraformOutput]): ProvisionResult =
    ProvisionResult(ProvisioningStatus.Completed, componentToken, Seq.empty, outputs)

  def completed(changes: Json, logs: Seq[Log]): ProvisionResult =
    ProvisionResult(
      ProvisioningStatus.Completed,
      ComponentToken(""),
      Seq.empty,
      Seq.empty,
      logs = logs,
      changes = changes
    )

  def failure(errors: Seq[ErrorMessage]): ProvisionResult =
    ProvisionResult(ProvisioningStatus.Failed, ComponentToken(""), errors)

  def failureWithLogs(logs: Seq[Log]): ProvisionResult =
    ProvisionResult(ProvisioningStatus.Failed, ComponentToken(""), Seq.empty, Seq.empty, logs)

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
  errors: Seq[ErrorMessage],
  outputs: Seq[TerraformOutput] = Seq.empty,
  logs: Seq[Log] = Seq.empty,
  changes: Json = Json.Null
) {

  /** Returns true if the provisioning of the component is successful.
    */
  def isSuccessful: Boolean = provisioningStatus != ProvisioningStatus.Failed
}
