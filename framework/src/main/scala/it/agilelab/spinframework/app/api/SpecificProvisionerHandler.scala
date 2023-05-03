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
  }.handleError((f: Throwable) => Resource.ProvisionResponse.InternalServerError(systemError(f)))

  private def systemError(f: Throwable): SystemError = {
    logger.error("System Error", f)
    SystemError(Option(f.getMessage).getOrElse("System Error"))
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
  }.handleError((f: Throwable) => Resource.UnprovisionResponse.InternalServerError(systemError(f)))

  override def validate(respond: Resource.ValidateResponse.type)(
    body: ProvisioningRequest
  ): IO[Resource.ValidateResponse] = IO.blocking {
    val compileResult = compile.doCompile(YamlDescriptor(body.descriptor))
    val errors        = Option(ValidationErrorMapper.from(compileResult)).filter(_.errors.nonEmpty)
    Resource.ValidateResponse.Ok(ValidationResult(compileResult.isSuccess, errors))
  }.handleError((f: Throwable) => Resource.ValidateResponse.InternalServerError(systemError(f)))

  override def getStatus(respond: Resource.GetStatusResponse.type)(token: String): IO[Resource.GetStatusResponse] =
    IO.blocking {
      val status: ProvisioningStatus = checkStatus.statusOf(ComponentToken(token))
      val statusDto                  = ProvisioningStatusMapper.from(status)
      Resource.GetStatusResponse.Ok(statusDto)
    }.handleError((f: Throwable) => Resource.GetStatusResponse.InternalServerError(systemError(f)))

}
