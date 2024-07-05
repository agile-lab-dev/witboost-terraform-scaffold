package it.agilelab.spinframework.app.features.provision

/** Represents the status of a provisioning request.
  *
  * This information is used when the request is executed in an asynchronous way.
  */
class ProvisioningStatus {
  override def toString: String = ProvisioningStatus.asString(this)
}

object ProvisioningStatus {
  val Completed: ProvisioningStatus = new ProvisioningStatus
  val Failed: ProvisioningStatus    = new ProvisioningStatus
  val Running: ProvisioningStatus   = new ProvisioningStatus

  def asString(status: ProvisioningStatus): String = status match {
    case Completed => "Completed"
    case Failed    => "Failed"
    case Running   => "Running"
    case _         => super.toString
  }
}
