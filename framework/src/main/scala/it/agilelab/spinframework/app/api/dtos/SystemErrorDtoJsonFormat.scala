package it.agilelab.spinframework.app.api.dtos

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

trait SystemErrorDtoJsonFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val systemErrorDtoFormat: RootJsonFormat[SystemErrorDto] = jsonFormat1(SystemErrorDto)

}
