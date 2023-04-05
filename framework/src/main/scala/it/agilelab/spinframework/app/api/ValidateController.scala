package it.agilelab.spinframework.app.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import it.agilelab.spinframework.app.api.dtos._
import it.agilelab.spinframework.app.api.dtos.{
  ProvisionRequestDto,
  ProvisionRequestDtoJsonFormat,
  ValidateResponseDto,
  ValidateResponseDtoJsonFormat,
  ValidationErrorDtoObj
}
import it.agilelab.spinframework.app.api.server.Controller
import it.agilelab.spinframework.app.features.compiler.{ Compile, YamlDescriptor }

class ValidateController(compile: Compile)
    extends Controller
    with ValidateResponseDtoJsonFormat
    with ProvisionRequestDtoJsonFormat {

  def route(): Route =
    path(basePath / "validate") {
      post {
        entity(as[ProvisionRequestDto])(request =>
          catchSystemErrors {
            val compileResult = compile.doCompile(YamlDescriptor(request.descriptor))
            val errors        = ValidationErrorDtoObj.from(compileResult.errors)
            val response      = ValidateResponseDto(compileResult.isSuccess, errors)
            complete(StatusCodes.OK, response)
          }
        )
      }
    }

}
