package it.agilelab.spinframework.app.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ Route, StandardRoute }
import it.agilelab.spinframework.app.api.dtos._
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus._
import it.agilelab.spinframework.app.features.provision.{ Provision, ProvisionResult }
import it.agilelab.spinframework.app.api.dtos.{
  ProvisionRequestDto,
  ProvisionRequestDtoJsonFormat,
  ProvisioningStatusDtoJsonFormat,
  ProvisioningStatusDtoObj,
  ValidationErrorDtoJsonFormat,
  ValidationErrorDtoObj
}
import it.agilelab.spinframework.app.api.server.Controller
import it.agilelab.spinframework.app.features.compiler.YamlDescriptor
import it.agilelab.spinframework.app.features.provision.{ Provision, ProvisionResult }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

class ProvisionController(provision: Provision)
    extends Controller
    with ProvisionRequestDtoJsonFormat
    with ValidationErrorDtoJsonFormat
    with ProvisioningStatusDtoJsonFormat {

  def route(): Route =
    concat(
      path(basePath / "provision") {
        post {
          entity(as[ProvisionRequestDto]) { request =>
            catchSystemErrors {
              val descriptor = YamlDescriptor(request.descriptor)
              val result     = provision.doProvisioning(descriptor)
              response(result)
            }
          }
        }
      },
      path(basePath / "unprovision") {
        post {
          entity(as[ProvisionRequestDto]) { request =>
            catchSystemErrors {
              val descriptor = YamlDescriptor(request.descriptor)
              val result     = provision.doUnprovisioning(descriptor)
              response(result)
            }
          }
        }
      }
    )

  private def response(result: ProvisionResult): StandardRoute =
    result.provisioningStatus match {
      case Completed => complete(StatusCodes.OK, ProvisioningStatusDtoObj.from(result.provisioningStatus))
      case Failed    => complete(StatusCodes.BadRequest, ValidationErrorDtoObj.from(result.errors))
      case Running   => complete(StatusCodes.Accepted, result.componentToken.asString)
    }
}
