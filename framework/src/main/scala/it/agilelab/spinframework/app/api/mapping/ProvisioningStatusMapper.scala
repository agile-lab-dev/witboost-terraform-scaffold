package it.agilelab.spinframework.app.api.mapping

import it.agilelab.spinframework.app.api.generated.definitions.{ Info, ProvisioningStatus }
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus._
import it.agilelab.spinframework.app.features.provision.ProvisionResult

object ProvisioningStatusMapper {

  def from(result: ProvisionResult): ProvisioningStatus =
    result.provisioningStatus match {
      case Completed =>
        ProvisioningStatus(ProvisioningStatus.Status.Completed, "", info = ProvisioningInfoMapper.from(result))
      case Failed    => ProvisioningStatus(ProvisioningStatus.Status.Failed, "")
      case Running   => ProvisioningStatus(ProvisioningStatus.Status.Running, "")
    }

  def from(status: it.agilelab.spinframework.app.features.provision.ProvisioningStatus): ProvisioningStatus =
    status match {
      case Completed => ProvisioningStatus(ProvisioningStatus.Status.Completed, "")
      case Failed    => ProvisioningStatus(ProvisioningStatus.Status.Failed, "")
      case Running   => ProvisioningStatus(ProvisioningStatus.Status.Running, "")
    }

}
