package it.agilelab.spinframework.app.api.mapping

import io.circe.Json
import it.agilelab.spinframework.app.api.generated.definitions.ReverseProvisioningStatus
import it.agilelab.spinframework.app.features.provision.ProvisionResult
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus.{ Completed, Failed, Running }

object ReverseProvisioningStatusMapper {
  def from(result: ProvisionResult): ReverseProvisioningStatus =
    result.provisioningStatus match {
      case Completed =>
        ReverseProvisioningStatus(
          ReverseProvisioningStatus.Status.Completed,
          result.changes,
          logs = Some(result.logs.toVector)
        )
      case Failed    =>
        ReverseProvisioningStatus(ReverseProvisioningStatus.Status.Failed, Json.Null, logs = Some(result.logs.toVector))
      case Running   => ReverseProvisioningStatus(ReverseProvisioningStatus.Status.Running, Json.Null)
    }
}
