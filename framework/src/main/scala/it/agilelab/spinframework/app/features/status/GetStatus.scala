package it.agilelab.spinframework.app.features.status

import it.agilelab.spinframework.app.features.provision.{ ComponentToken, ProvisioningStatus }

/** This trait represents the provisioning status
  * of the request which is identified by an input token.
  *
  * As a client of the framework you must extend this trait and provide
  * your own implementation.
  *
  * If the provision operation always executes synchronous commands on the
  * cloud provider, this trait is meaningless and is meant to never be invoked.
  */
trait GetStatus {

  /** Analyzes the token, extracts the necessary
    * information from it and uses them to check on the cloud provider
    * if the referred resources are actually in place.
    *
    * @param token an identifier of the component
    * @return the status of the provisioning request
    */
  def statusOf(token: ComponentToken): ProvisioningStatus
}
