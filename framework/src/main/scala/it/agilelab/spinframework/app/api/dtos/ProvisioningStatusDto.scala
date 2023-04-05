package it.agilelab.spinframework.app.api.dtos

import it.agilelab.spinframework.app.features.provision.ProvisioningStatus._
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus

object ProvisioningStatusDtoObj {
  val COMPLETED: ProvisioningStatusDto = ProvisioningStatusDto("COMPLETED")
  val FAILED: ProvisioningStatusDto    = ProvisioningStatusDto("FAILED")
  val RUNNING: ProvisioningStatusDto   = ProvisioningStatusDto("RUNNING")

  def from(status: ProvisioningStatus): ProvisioningStatusDto =
    status match {
      case Completed => COMPLETED
      case Failed    => FAILED
      case Running   => RUNNING
      case _         => throw new IllegalStateException(s"Invalid status: $status")
    }
}

case class ProvisioningStatusDto(status: String)
