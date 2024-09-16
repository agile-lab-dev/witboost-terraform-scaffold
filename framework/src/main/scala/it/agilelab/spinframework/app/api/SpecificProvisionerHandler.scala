package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.definitions.{
  ProvisioningRequest,
  ReverseProvisioningRequest,
  SystemError,
  UpdateAclRequest,
  ValidationError,
  ValidationRequest,
  ValidationResult
}
import it.agilelab.spinframework.app.api.generated.{ Handler, Resource }
import it.agilelab.spinframework.app.api.mapping.{
  ProvisioningStatusMapper,
  ReverseProvisioningStatusMapper,
  ValidationErrorMapper
}
import it.agilelab.spinframework.app.features.compiler.{ Compile, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus.{ Completed, Failed, Running }
import it.agilelab.spinframework.app.features.provision.{ AsyncProvision, ComponentToken }
import it.agilelab.spinframework.app.features.status.GetStatus
import org.slf4j.{ Logger, LoggerFactory }

import scala.None

class SpecificProvisionerHandler(provision: AsyncProvision, compile: Compile, checkStatus: GetStatus)
    extends Handler[IO] {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  sealed trait OperationType
  case object Provision   extends OperationType
  case object Unprovision extends OperationType
  case object UpdateAcl   extends OperationType
  case object Validate    extends OperationType
  case object Status      extends OperationType

  override def provision(
    respond: Resource.ProvisionResponse.type
  )(body: ProvisioningRequest): IO[Resource.ProvisionResponse] = {
    val descriptor = YamlDescriptor(body.descriptor)
    provision
      .doProvisioning(descriptor)
      .map { result =>
        result.provisioningStatus match {
          case Running   => Resource.ProvisionResponse.Accepted(result.componentToken.asString)
          case Completed => Resource.ProvisionResponse.Ok(ProvisioningStatusMapper.from(result))
          case Failed    => Resource.ProvisionResponse.BadRequest(ValidationErrorMapper.from(result))
        }
      }
      .handleError((f: Throwable) => Resource.ProvisionResponse.InternalServerError(systemError(f, Provision)))
  }

  private def systemError(f: Throwable, operationType: OperationType): SystemError = {
    logger.error("System Error", f)
    operationType match {
      case Provision   =>
        SystemError(
          s"An unexpected error occurred while processing the provision request. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
        )
      case Unprovision =>
        SystemError(
          s"An unexpected error occurred while processing the unprovision request. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
        )
      case UpdateAcl   =>
        SystemError(
          s"An unexpected error occurred while processing the updateAcl request. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
        )
      case Validate    =>
        SystemError(
          s"An unexpected error occurred while validating the request. Please try again later. If the issue still persists, contact the platform team for assistance! Detailed error: ${f.getMessage}"
        )
      case Status      =>
        SystemError(
          s"An unexpected error occurred while retrieving the status. Please try again later. If the issue still persists, contact the platform team for assistance!  Detailed error: ${f.getMessage}"
        )
    }
  }

  override def unprovision(respond: Resource.UnprovisionResponse.type)(
    body: ProvisioningRequest
  ): IO[Resource.UnprovisionResponse] = {
    val descriptor = YamlDescriptor(body.descriptor)
    val removeData = body.removeData
    provision
      .doUnprovisioning(descriptor, removeData)
      .map { result =>
        result.provisioningStatus match {
          case Running   => Resource.UnprovisionResponse.Accepted(result.componentToken.asString)
          case Completed => Resource.UnprovisionResponse.Ok(ProvisioningStatusMapper.from(result))
          case Failed    =>
            Resource.UnprovisionResponse.BadRequest(ValidationErrorMapper.from(result))
        }
      }
      .handleError((f: Throwable) => Resource.UnprovisionResponse.InternalServerError(systemError(f, Unprovision)))
  }

  override def validate(respond: Resource.ValidateResponse.type)(
    body: ProvisioningRequest
  ): IO[Resource.ValidateResponse] = {
    val descriptor = YamlDescriptor(body.descriptor)
    provision
      .doValidate(descriptor)
      .map { result =>
        if (result.isSuccessful) {
          Resource.ValidateResponse.Ok(ValidationResult(valid = true))
        } else {
          Resource.ValidateResponse.Ok(ValidationResult(valid = false, Some(ValidationErrorMapper.from(result))))
        }
      }
      .handleError((f: Throwable) => Resource.ValidateResponse.InternalServerError(systemError(f, Validate)))
  }

  override def getStatus(respond: Resource.GetStatusResponse.type)(token: String): IO[Resource.GetStatusResponse] =
    checkStatus
      .statusOf(ComponentToken(token))
      .map { status =>
        status
          .fold[Resource.GetStatusResponse](
            Resource.GetStatusResponse.InternalServerError(SystemError(s"Couldn't find operation for token '$token'"))
          ) { result =>
            val statusDto = ProvisioningStatusMapper.from(result)
            Resource.GetStatusResponse.Ok(statusDto)
          }
      }
      .handleError((f: Throwable) => Resource.GetStatusResponse.InternalServerError(systemError(f, Status)))

  override def updateacl(
    respond: Resource.UpdateaclResponse.type
  )(body: UpdateAclRequest): IO[Resource.UpdateaclResponse] =
    provision
      .doUpdateAcl(body.provisionInfo, body.refs.toSet)
      .map { result =>
        result.provisioningStatus match {
          case Running   => Resource.UpdateaclResponse.Accepted(result.componentToken.asString)
          case Completed => Resource.UpdateaclResponse.Ok(ProvisioningStatusMapper.from(result))
          case Failed    =>
            Resource.UpdateaclResponse.BadRequest(ValidationErrorMapper.from(result))
        }
      }
      .handleError((f: Throwable) => Resource.UpdateaclResponse.InternalServerError(systemError(f, UpdateAcl)))

  override def asyncValidate(respond: Resource.AsyncValidateResponse.type)(
    body: ValidationRequest
  ): IO[Resource.AsyncValidateResponse] = IO {
    Resource.AsyncValidateResponse.InternalServerError(SystemError("The asyncValidate operation is not supported"))
  }

  override def getValidationStatus(respond: Resource.GetValidationStatusResponse.type)(
    token: String
  ): IO[Resource.GetValidationStatusResponse]    = IO {
    Resource.GetValidationStatusResponse.InternalServerError(
      SystemError("The validationStatus operation is not supported")
    )
  }
  override def runReverseProvisioning(respond: Resource.RunReverseProvisioningResponse.type)(
    body: ReverseProvisioningRequest
  ): IO[Resource.RunReverseProvisioningResponse] =
    body match {
      case ReverseProvisioningRequest(useCaseTemplateId, _, Some(params), Some(cInfo)) =>
        provision.doReverse(useCaseTemplateId, cInfo, params).map { result =>
          result.provisioningStatus match {
            case Running   => Resource.RunReverseProvisioningResponse.Accepted(result.componentToken.asString)
            case Completed => Resource.RunReverseProvisioningResponse.Ok(ReverseProvisioningStatusMapper.from(result))
            case Failed    =>
              Resource.RunReverseProvisioningResponse.Ok(ReverseProvisioningStatusMapper.from(result))
          }
        }
      case _                                                                           =>
        IO {
          logger.error(
            "It was not possible to parse the ReverseProvisioningRequest, CatalogInfo or InputParams are not present"
          )
          Resource.RunReverseProvisioningResponse.BadRequest(
            ValidationError(Vector("CatalogInfo or InputParams are not present"))
          )
        }
    }

  override def getReverseProvisioningStatus(respond: Resource.GetReverseProvisioningStatusResponse.type)(
    token: String
  ): IO[Resource.GetReverseProvisioningStatusResponse] =
    checkStatus
      .statusOf(ComponentToken(token))
      .map { status =>
        status
          .fold[Resource.GetReverseProvisioningStatusResponse](
            Resource.GetReverseProvisioningStatusResponse
              .InternalServerError(SystemError(s"Couldn't find operation for token '$token'"))
          ) { result =>
            val statusDto = ReverseProvisioningStatusMapper.from(result)
            Resource.GetReverseProvisioningStatusResponse.Ok(statusDto)
          }
      }
      .handleError((f: Throwable) =>
        Resource.GetReverseProvisioningStatusResponse.InternalServerError(systemError(f, Status))
      )
}
