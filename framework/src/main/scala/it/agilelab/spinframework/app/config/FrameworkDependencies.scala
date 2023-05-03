package it.agilelab.spinframework.app.config

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits.toSemigroupKOps
import it.agilelab.spinframework.app.api.routes.HealthCheck
import it.agilelab.spinframework.app.api.generated.{ Handler, Resource }
import it.agilelab.spinframework.app.api.SpecificProvisionerHandler
import it.agilelab.spinframework.app.features.compiler.{ CompileService, Parser, ParserFactory }
import it.agilelab.spinframework.app.features.provision.ProvisionService
import org.http4s.server.middleware.Logger
import org.http4s.{ Request, Response }

/** Used to link a specific provisioner's configuration
  * to the internal instances of the framework classes.
  *
  * @param specific configuration of a specific provisioner
  */
final class FrameworkDependencies(specific: SpecificProvisionerDependencies) {

  private val parser: Parser                  = ParserFactory.parser()
  private val compileService                  = new CompileService(parser, specific.descriptorValidator)
  private val provisionService                = new ProvisionService(compileService, specific.cloudProvider)
  private val provisionerHandler: Handler[IO] =
    new SpecificProvisionerHandler(provisionService, compileService, specific.getStatus)
  private val provisionerService              = new Resource[IO]().routes(provisionerHandler)
  private val healthCheckService              = HealthCheck.routes[IO]()
  private val combinedServices                = provisionerService <+> healthCheckService
  private val loggerService                   = Logger.httpRoutes[IO](
    logHeaders = false,
    logBody = true,
    redactHeadersWhen = _ => false,
    logAction = None
  )(combinedServices)

  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = loggerService.orNotFound
}
