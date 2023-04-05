package it.agilelab.spinframework.app.api.dtos

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

trait ValidationErrorDtoJsonFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val format: RootJsonFormat[ValidationErrorDto] = jsonFormat1(ValidationErrorDto)
}
