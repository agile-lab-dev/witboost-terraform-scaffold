package it.agilelab.spinframework.app.api.dtos

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

trait ProvisionRequestDtoJsonFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val provisionRequestFormat: RootJsonFormat[ProvisionRequestDto] = jsonFormat1(ProvisionRequestDto)
}
