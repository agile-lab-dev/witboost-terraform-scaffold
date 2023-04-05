package it.agilelab.spinframework.app.api.dtos

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

trait ProvisioningStatusDtoJsonFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val provisioningStatusDtoFormat: RootJsonFormat[ProvisioningStatusDto] = jsonFormat1(ProvisioningStatusDto)
}
