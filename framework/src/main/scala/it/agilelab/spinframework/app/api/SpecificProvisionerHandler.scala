package it.agilelab.spinframework.app.api

import cats.effect.IO
import it.agilelab.spinframework.app.api.generated.definitions.{ ProvisioningRequest, SystemError, ValidationResult }
import it.agilelab.spinframework.app.api.generated.{ Handler, Resource }
import it.agilelab.spinframework.app.api.mapping.{ ProvisioningStatusMapper, ValidationErrorMapper }
import it.agilelab.spinframework.app.features.compiler.{ Compile, YamlDescriptor }
import it.agilelab.spinframework.app.features.provision.ProvisioningStatus.{ Completed, Failed, Running }
import it.agilelab.spinframework.app.features.provision.{ ComponentToken, Provision, ProvisioningStatus }
import it.agilelab.spinframework.app.features.status.GetStatus
import org.slf4j.{ Logger, LoggerFactory }

class SpecificProvisionerHandler(provision: Provision, compile: Compile, checkStatus: GetStatus) extends Handler[IO] {

  final private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  sealed trait OperationType
  case object Provision   extends OperationType
  case object Unprovision extends OperationType
  case object Validate    extends OperationType
  case object Status      extends OperationType

  override def provision(
    respond: Resource.ProvisionResponse.type
  )(body: ProvisioningRequest): IO[Resource.ProvisionResponse] = IO.blocking {
    val descriptor = YamlDescriptor(body.descriptor)
    val result     = provision.doProvisioning(descriptor)
    result.provisioningStatus match {
      case Running   => Resource.ProvisionResponse.Accepted(result.componentToken.asString)
      case Completed => Resource.ProvisionResponse.Ok(ProvisioningStatusMapper.from(result))
      case Failed    =>
        Resource.ProvisionResponse.BadRequest(ValidationErrorMapper.from(result))
    }
  }.handleError((f: Throwable) => Resource.ProvisionResponse.InternalServerError(systemError(f, Provision)))

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
  ): IO[Resource.UnprovisionResponse] = IO.blocking {
    val descriptor = YamlDescriptor(body.descriptor)
    val result     = provision.doUnprovisioning(descriptor)
    result.provisioningStatus match {
      case Running   => Resource.UnprovisionResponse.Accepted(result.componentToken.asString)
      case Completed => Resource.UnprovisionResponse.Ok(ProvisioningStatusMapper.from(result))
      case Failed    =>
        Resource.UnprovisionResponse.BadRequest(ValidationErrorMapper.from(result))
    }
  }.handleError((f: Throwable) => Resource.UnprovisionResponse.InternalServerError(systemError(f, Unprovision)))

  override def validate(respond: Resource.ValidateResponse.type)(
    body: ProvisioningRequest
  ): IO[Resource.ValidateResponse] = IO.blocking {
    val compileResult = compile.doCompile(YamlDescriptor(body.descriptor))
    val errors        = Option(ValidationErrorMapper.from(compileResult)).filter(_.errors.nonEmpty)
    Resource.ValidateResponse.Ok(ValidationResult(compileResult.isSuccess, errors))
  }.handleError((f: Throwable) => Resource.ValidateResponse.InternalServerError(systemError(f, Validate)))

  override def getStatus(respond: Resource.GetStatusResponse.type)(token: String): IO[Resource.GetStatusResponse] =
    IO.blocking {
      val status: ProvisioningStatus = checkStatus.statusOf(ComponentToken(token))
      val statusDto                  = ProvisioningStatusMapper.from(status)
      Resource.GetStatusResponse.Ok(statusDto)
    }.handleError((f: Throwable) => Resource.GetStatusResponse.InternalServerError(systemError(f, Status)))

}
