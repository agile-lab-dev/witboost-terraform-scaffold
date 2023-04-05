package it.agilelab.spinframework.app.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.agilelab.spinframework.app.api.dtos.{ ProvisioningStatusDtoJsonFormat, ProvisioningStatusDtoObj }
import it.agilelab.spinframework.app.api.server.Controller
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, ProvisioningStatus }
import it.agilelab.spinframework.app.features.status.GetStatus

class GetStatusController(checkStatus: GetStatus) extends Controller with ProvisioningStatusDtoJsonFormat {

  def route(): Route =
    path(basePath / "status" / Segment) { token =>
      get {
        catchSystemErrors {
          val status: ProvisioningStatus = checkStatus.statusOf(ComponentToken(token))
          val statusDto                  = ProvisioningStatusDtoObj.from(status)
          complete(StatusCodes.OK, statusDto)
        }
      }
    }
}
