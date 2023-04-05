package it.agilelab.spinframework.app.api.dtos

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

trait ValidateResponseDtoJsonFormat extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val validationErrorDtoFormat: RootJsonFormat[ValidationErrorDto]  = jsonFormat1(ValidationErrorDto)
  implicit val validationResponseFormat: RootJsonFormat[ValidateResponseDto] = jsonFormat2(ValidateResponseDto)

}
